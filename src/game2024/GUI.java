package game2024;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;
import javafx.util.Pair;

public class GUI extends Application {

    private int currentPlayerIndex = 0; // Index of the player who has the token

    private Stage primaryStage;

    public static final int size = 20;
    public static final int scene_height = size * 20 + 100;
    public static final int scene_width = size * 20 + 200;

    public static Image image_floor;
    public static Image image_wall;
    public static Image hero_right, hero_left, hero_up, hero_down;

    public static Player me;
    public static List<Player> players = new ArrayList<Player>();

    private Label[][] fields;
    private TextArea scoreList;

    private ArrayList<Socket> sockets = new ArrayList<>();
    private ArrayList<PrintWriter> printers = new ArrayList<>();

    private String[] board = {    // 20x20
            "wwwwwwwwwwwwwwwwwwww",
            "w        ww        w",
            "w w  w  www w  w  ww",
            "w w  w   ww w  w  ww",
            "w  w               w",
            "w w w w w w w  w  ww",
            "w w     www w  w  ww",
            "w w     w w w  w  ww",
            "w   w w  w  w  w   w",
            "w     w  w  w  w   w",
            "w ww ww        w  ww",
            "w  w w    w    w  ww",
            "w        ww w  w  ww",
            "w         w w  w  ww",
            "w        w     w  ww",
            "w  w              ww",
            "w  w www  w w  ww ww",
            "w w      ww w     ww",
            "w   w   ww  w      w",
            "wwwwwwwwwwwwwwwwwwww"
    };

    // -------------------------------------------
    // | Maze: (0,0)              | Score: (1,0) |
    // |-----------------------------------------|
    // | boardGrid (0,1)          | scorelist    |
    // |                          | (1,1)        |
    // -------------------------------------------

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Open the new game window
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20));
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        // Player Name input
        Label playerNameLabel = new Label("Player Name:");
        TextField playerNameField = new TextField();
        playerNameField.setPromptText("Enter your name");

        // Server IP Address input
        Label serverIPLabel = new Label("Clients IP addresses:");
        TextArea clientsTextArea = new TextArea();
        clientsTextArea.setPromptText("Enter server IP addresses of other clients");

        // Join Game button
        Button joinButton = new Button("Join Game");
        joinButton.setOnAction(e -> {
            String playerName = playerNameField.getText();
            String[] clientIPs = clientsTextArea.getText().split(System.lineSeparator());
            startGame(playerName, clientIPs);
            dialog.close();
        });

        // Add components to the gridPane
        gridPane.add(playerNameLabel, 0, 0);
        gridPane.add(playerNameField, 1, 0);
        gridPane.add(serverIPLabel, 0, 1, 2, 1);
        gridPane.add(clientsTextArea, 0, 2, 2, 1);
        gridPane.add(joinButton, 0, 3, 2, 1);

        Scene scene = new Scene(gridPane, 400, 200);
        dialog.setScene(scene);
        dialog.show();
    }

    public void connect(String[] clientIPs) {
        for (String ip : clientIPs) {
            if (ip.isEmpty()) {
                continue;
            }
            Socket sock;
            String[] split = ip.split(":");
            try {
                sock = new Socket(split[0], Integer.parseInt(split[1]));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sockets.add(sock);
            setupPlayerSocket(sock);
        }

        // Setup thread for accepting connections from other players
        String port = System.getenv("SERVER_SOCKET_PORT");
        if (port.isEmpty()) {
            port = "8000";
        }
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(Integer.parseInt(port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            while(true) {
                try {
                    Socket incomingSock = serverSocket.accept();
                    setupPlayerSocket(incomingSock);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void setupPlayerSocket(Socket incomingSock) {
        try {
            PrintWriter p = new PrintWriter(incomingSock.getOutputStream(), true);
            this.printers.add(p);
            identify(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(incomingSock.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void handleMessage(String message) {
        System.out.println("Got message: " + message);

        // Extract the command
        String[] split = message.split(" ");

        switch (split[0]) {
            case "IDENTIFY":
                handleIdentify(split[1], split[2], split[3]);
                break;
            case "POINT":
                handlePoint(split[1], Integer.parseInt(split[2]));
                break;
            case "MOVE":
                handleMove(split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]), split[4]);
                break;
        }
    }

    public synchronized void handleMove(String playerName, int xpos, int ypos, String direction) {

        // Find the player to change points
        for (Player p : players) {
            if (p.name.equals(playerName)) {
                // Use Platform.runlater as we modify fx from a thread
                Platform.runLater(() -> playerMoved(p, xpos - p.getXpos(), ypos - p.getYpos(), direction));
            }
        }

    }

    public void handleIdentify(String name, String xpos, String ypos) {
        addPlayer(new Player(name, Integer.parseInt(xpos), Integer.parseInt(ypos), "up"));
    }

    public void handlePoint(String playerName, int point) {
        // Find the player to change points
        for (Player p : players) {
            if (p.name.equals(playerName)) {
                p.point += point;
            }
        }

        scoreList.setText(getScoreList());
    }

    private void sendMessage(String message) {
        System.out.println("Sending message: " + message);
        for (PrintWriter p : printers) {
            p.println(message);
        }
    }

    public void startGame(String playerName, String[] clientIPs) {

        try {
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(0, 10, 0, 10));

            Text mazeLabel = new Text("Maze:");
            mazeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

            Text scoreLabel = new Text("Score:");
            scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

            scoreList = new TextArea();

            GridPane boardGrid = new GridPane();

            image_wall = new Image(getClass().getResourceAsStream("Image/wall4.png"), size, size, false, false);
            image_floor = new Image(getClass().getResourceAsStream("Image/floor1.png"), size, size, false, false);

            hero_right = new Image(getClass().getResourceAsStream("Image/heroRight.png"), size, size, false, false);
            hero_left = new Image(getClass().getResourceAsStream("Image/heroLeft.png"), size, size, false, false);
            hero_up = new Image(getClass().getResourceAsStream("Image/heroUp.png"), size, size, false, false);
            hero_down = new Image(getClass().getResourceAsStream("Image/heroDown.png"), size, size, false, false);

            fields = new Label[20][20];
            for (int j = 0; j < 20; j++) {
                for (int i = 0; i < 20; i++) {
                    switch (board[j].charAt(i)) {
                        case 'w':
                            fields[i][j] = new Label("", new ImageView(image_wall));
                            break;
                        case ' ':
                            fields[i][j] = new Label("", new ImageView(image_floor));
                            break;
                        default:
                            throw new Exception("Illegal field value: " + board[j].charAt(i));
                    }
                    boardGrid.add(fields[i][j], i, j);
                }
            }
            scoreList.setEditable(false);

            grid.add(mazeLabel, 0, 0);
            grid.add(scoreLabel, 1, 0);
            grid.add(boardGrid, 0, 1);
            grid.add(scoreList, 1, 1);

            Scene scene = new Scene(grid, scene_width, scene_height);
            primaryStage.setScene(scene);
            primaryStage.show();

            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                switch (event.getCode()) {
                    case UP:
                        playerMoved(me, 0, -1, "up");
                        break;
                    case DOWN:
                        playerMoved(me, 0, +1, "down");
                        break;
                    case LEFT:
                        playerMoved(me, -1, 0, "left");
                        break;
                    case RIGHT:
                        playerMoved(me,+1, 0, "right");
                        break;
                    default:
                        break;
                }
            });

            // Setting up player
            Pair<Integer, Integer> freePoint = getFreePoint();
            me = new Player(playerName, freePoint.getKey(), freePoint.getValue(), "up");
            addPlayer(me);

            // Setup network
            connect(clientIPs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void identify(PrintWriter p) {
        p.println(String.format("IDENTIFY %s %d %d", me.name, me.xpos, me.ypos));
    }

    public void addPlayer(Player p)  {
        Platform.runLater(() -> { // use Platform.runLater as we modify fx from a thread
            fields[p.xpos][p.ypos].setGraphic(new ImageView(hero_up));
            scoreList.setText(getScoreList());
        });
        players.add(p);
    }

    public Pair<Integer, Integer> getFreePoint() {
        while(true) {
            int x = ThreadLocalRandom.current().nextInt(0, 20);
            int y = ThreadLocalRandom.current().nextInt(0, 20);

            if (board[x].charAt(y) == 'w') {
                continue;
            }

            if (getPlayerAt(x, y) != null) {
                continue;
            }

            return new Pair<>(x, y);
        }
    }

    public void adjustPoints(Player p, int points) {
        p.addPoints(points);
        sendMessage(String.format("POINT %s %d", p.name, points));
        scoreList.setText(getScoreList());
    }

    public void playerMoved(Player player, int delta_x, int delta_y, String direction) {
        player.direction = direction;
        int x = player.getXpos(), y = player.getYpos();

        if (board[y + delta_y].charAt(x + delta_x) == 'w') {
            adjustPoints(player, -1);
        } else {
            Player p = getPlayerAt(x + delta_x, y + delta_y);
            if (p != null) {
                adjustPoints(player, 10);
                adjustPoints(p, -10);
            } else {
                adjustPoints(player, 1);

                fields[x][y].setGraphic(new ImageView(image_floor));
                x += delta_x;
                y += delta_y;

                if (direction.equals("right")) {
                    fields[x][y].setGraphic(new ImageView(hero_right));
                }
                ;
                if (direction.equals("left")) {
                    fields[x][y].setGraphic(new ImageView(hero_left));
                }
                ;
                if (direction.equals("up")) {
                    fields[x][y].setGraphic(new ImageView(hero_up));
                }
                ;
                if (direction.equals("down")) {
                    fields[x][y].setGraphic(new ImageView(hero_down));
                }
                ;

                player.setXpos(x);
                player.setYpos(y);

                if (player == me) {
                    sendMessage(String.format("MOVE %s %d %d %s", me.name, me.xpos, me.ypos, me.direction));
                }
            }
        }
    }

    public String getScoreList() {
        StringBuffer b = new StringBuffer(100);
        for (Player p : players) {
            b.append(p + "\r\n");
        }
        return b.toString();
    }

    public Player getPlayerAt(int x, int y) {
        for (Player p : players) {
            if (p.getXpos() == x && p.getYpos() == y) {
                return p;
            }
        }
        return null;
    }


}

