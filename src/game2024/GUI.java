package game2024;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.SpotLight;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;

public class GUI extends Application {

	public static final int size = 20;
	public static final int scene_height = size * 20 + 100;
	public static final int scene_width = size * 20 + 200;

	public static Image image_floor;
	public static Image image_wall;
	public static Image hero_right,hero_left,hero_up,hero_down;

	public static Player me;
	public static List<Player> players = new ArrayList<Player>();

	private Label[][] fields;
	private TextArea scoreList;
	private TextArea chatDisplay;
	private TextField chatInput;


	private List<PrintWriter> printers = new ArrayList<>();
	private List<Socket> sockets = new ArrayList<>();
	private ServerSocket serverSocket;

	private  String[] board = {    // 20x20
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

	/**
	 *  Instructions:
	 *  Player 1 starts the game, and doesn't enter any IP og ports in the dialog box (server)
	 *  Player 2 enters player 1's IP and port.
	 *	Player 3 enters both player 1's and player 2's IP-addresses and ports.
	 */

	@Override
	public void start(Stage primaryStage) {
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

			image_wall  = new Image(getClass().getResourceAsStream("Image/wall4.png"),size,size,false,false);
			image_floor = new Image(getClass().getResourceAsStream("Image/floor1.png"),size,size,false,false);

			hero_right  = new Image(getClass().getResourceAsStream("Image/heroRight.png"),size,size,false,false);
			hero_left   = new Image(getClass().getResourceAsStream("Image/heroLeft.png"),size,size,false,false);
			hero_up     = new Image(getClass().getResourceAsStream("Image/heroUp.png"),size,size,false,false);
			hero_down   = new Image(getClass().getResourceAsStream("Image/heroDown.png"),size,size,false,false);

			fields = new Label[20][20];
			for (int j=0; j<20; j++) {
				for (int i=0; i<20; i++) {
					switch (board[j].charAt(i)) {
						case 'w':
							fields[i][j] = new Label("", new ImageView(image_wall));
							break;
						case ' ':
							fields[i][j] = new Label("", new ImageView(image_floor));
							break;
						default: throw new Exception("Illegal field value: "+board[j].charAt(i) );
					}
					boardGrid.add(fields[i][j], i, j);
				}
			}
			scoreList.setEditable(false);


			grid.add(mazeLabel,  0, 0);
			grid.add(scoreLabel, 1, 0);
			grid.add(boardGrid,  0, 1);
			grid.add(scoreList,  1, 1);

			chatDisplay = new TextArea();
			chatDisplay.setEditable(false);
			chatInput = new TextField();

			grid.add(chatDisplay, 0, 2, 2, 1);
			grid.add(chatInput, 0, 3, 2, 1);

			chatInput.setOnAction(event -> {
				String message = chatInput.getText();
				sendChatMessage(me.name, message);
				chatInput.clear();
			});


			Scene scene = new Scene(grid,scene_width,scene_height);
			primaryStage.setScene(scene);

			List<String> playerAddresses = showConnectionDialog();
			setupNetworking(playerAddresses);

			primaryStage.show();

			scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				switch (event.getCode()) {
					case UP:    playerMoved(0,-1,"up");    break;
					case DOWN:  playerMoved(0,+1,"down");  break;
					case LEFT:  playerMoved(-1,0,"left");  break;
					case RIGHT: playerMoved(+1,0,"right"); break;
					default: break;
				}
			});

			// Setting up standard players

			me = new Player("Orville",9,4,"up");
			players.add(me);
			fields[9][4].setGraphic(new ImageView(hero_up));

			scoreList.setText(getScoreList());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void playerMoved(int delta_x, int delta_y, String direction) {
		// TODO: update logic
		me.direction = direction;
		int x = me.getXpos(),y = me.getYpos();

		if (board[y + delta_y].charAt(x + delta_x) == 'w') {
			me.addPoints(-1);
			sendPointChange(me.name, me.point);
		} else {
			Player p = getPlayerAt(x + delta_x,y + delta_y);
			if (p != null) {
				me.addPoints(10);
				p.addPoints(-10);
				sendPointChange(me.name, me.point);
				sendPointChange(p.name, p.point);
			} else {
				me.addPoints(1);
				movePlayerOnGUI(x, y, delta_x, delta_y, direction);
				me.setXpos(x + delta_x);
				me.setYpos(y + delta_y);
				sendMove(me.name, me.getXpos(), me.getYpos(), direction);
			}
		}
		scoreList.setText(getScoreList());
	}

	private void movePlayerOnGUI(int x, int y, int delta_x, int delta_y, String dir) {
		fields[x][y].setGraphic(new ImageView(image_floor));
		Image directionImage = getDirectionImage(dir);
		fields[x + delta_x][y + delta_y].setGraphic(new ImageView(directionImage));
	}

	private Image getDirectionImage(String dir) {
		return switch (dir) {
			case "right" -> hero_right;
			case "left" -> hero_left;
			case "up" -> hero_up;
			case "down" -> hero_down;
			default -> hero_up;
		};
	}

	public String getScoreList() {
		StringBuffer b = new StringBuffer(100);
		for (Player p : players) {
			b.append(p+"\r\n");
		}
		return b.toString();
	}

	public Player getPlayerAt(int x, int y) {
		for (Player p : players) {
			if (p.getXpos()==x && p.getYpos()==y) {
				return p;
			}
		}
		return null;
	}

	private List<String> showConnectionDialog() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Connect to players");
		dialog.setHeaderText("Enter player addresses");
		dialog.setContentText("Format: ip:port, ...:");

		Optional<String> result = dialog.showAndWait();
		return result.map(s -> Arrays.asList(s.split(",\\s*"))).orElseGet(ArrayList::new);
	}

	private void setupNetworking(List<String> playerAddresses) {
		new Thread(() -> {
			setupServerSocket();
			acceptConnections();
		}).start();

		if (!playerAddresses.isEmpty()) {
			new Thread(() -> {
				try {
					Thread.sleep(1000);
					String playerName = me == null ? "Loshan" : me.name;
					int xPos = me == null ? 14 : me.getXpos();
					int yPos = me == null ? 15 : me.getYpos();
					connectToPlayers(playerAddresses.toArray(new String[0]), playerName, xPos, yPos);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}).start();
		}
	}

	private void acceptConnections() {
		try {
			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("Accepted connection from " + socket.getInetAddress());
				setupPlayerSocket(socket);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error accepting client connections");
		}
	}

	private void setupServerSocket() {
		int port = 7895;
		try {
			String envPort = System.getenv("SERVER_SOCKET_PORT");
			if (envPort != null) {
				port = Integer.parseInt(envPort);
			}

			serverSocket = new ServerSocket(port);
			System.out.println("Server started on port " + port);

			new Thread(this::acceptConnections).start();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot open server socket on port " + port, e);
		}
	}

	private void connectToPlayers(String[] addresses, String playerName, int xpos, int ypos) {
		for (String a : addresses) {
			try {
				String[] parts = a.split(":");
				if (parts.length < 2) {
					System.err.println("Invalid address format: " + a);
					continue;
				}
				Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
				setupPlayerSocket(socket);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println("IDENTIFY " + playerName + " " + xpos + " " + ypos);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port number in address: " + a);
			} catch (IOException e) {
				System.err.println("Cannot connect to " + a);
			}
		}
	}

	private void setupPlayerSocket(Socket socket) {
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			printers.add(out);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Thread readerThread = new Thread(() -> {
				String line;
				try {
					while ((line = in.readLine()) != null) {
						handleMessage(line);
					}
				} catch (IOException e) {
					System.err.println("Error reading message from socket");
					e.printStackTrace();
				}
			});
			readerThread.setDaemon(true);
			readerThread.start();
		} catch (IOException e) {
			throw new RuntimeException("Error setting up player socket", e);
		}
	}


	private void handleMessage(String message) {
		// TODO: Update logic to handle MOVE and POINT
		System.out.println("Message received: " + message);
		String[] split = message.split(" ");
		switch (split[0]) {
			case "IDENTIFY" -> handleIdentifyMessage(split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]));
			case "POINT" -> {
				if (split.length == 3)
					handlePoint(split[1], Integer.parseInt(split[2]));
			}
			case "MOVE" -> {
				if (split.length == 5)
					handleMove(split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]), split[4]);
			}
			case "CHAT" -> {
				if (split.length > 1) {
					String chatMessage = split[1];
					javafx.application.Platform.runLater(() -> chatDisplay.appendText(chatMessage + "\n"));
				}
			}
		}
	}

	//TODO: handlePoint & handleMove

	private void handlePoint(String playerName, int points) {
		Player player = players.stream().filter(p -> p.name.equals(playerName)).findFirst().orElse(null);
		if (player != null) {
			player.addPoints(points);
			javafx.application.Platform.runLater(() -> scoreList.setText(getScoreList()));
		}
	}

	private void handleMove(String playerName, int newX, int newY, String dir) {
		Player player = players.stream().filter(p -> p.name.equals(playerName)).findFirst().orElse(null);
		if (player != null && isMoveValid(newX, newY)) {
			updatePlayerPosition(player, newX, newY, dir);
		}
	}

	private boolean isMoveValid(int x, int y) {
		return x >= 0 && x < 20 && y >= 0 && y < 20 && board[y].charAt(x) != 'w' && getPlayerAt(x, y) == null;
	}

	private void handleIdentifyMessage(String playerName, int xpos, int ypos) {
		boolean exists = players.stream().anyMatch(p -> p.name.equals(playerName));
		if (!exists) {
			Player newPlayer = new Player(playerName, xpos, ypos, "up");
			players.add(newPlayer);
			System.out.println("Adding new player: " + playerName + " at (" + xpos + ", " + ypos + ")");
			javafx.application.Platform.runLater(() -> {
				fields[newPlayer.getXpos()][newPlayer.getYpos()].setGraphic(new ImageView(getDirectionImage(newPlayer.getDirection())));
				scoreList.setText(getScoreList());
			});
		} else {
			System.err.println("Player with name '" + playerName + "' already exists.");
		}
	}

	private void updatePlayerPositionOnGUI(Player player, int newX, int newY, String dir) {
		javafx.application.Platform.runLater(() -> {
			fields[player.getXpos()][player.getYpos()].setGraphic(new ImageView(image_floor));
			Image dirImage = getDirectionImage(dir);
			fields[newX][newY].setGraphic(new ImageView(dirImage));
		});
	}

	private void updatePlayerPosition(Player player, int newX, int newY, String dir) {
		javafx.application.Platform.runLater(() -> {
			player.setXpos(newX);
			player.setYpos(newY);
			player.setDirection(dir);
			updatePlayerPositionOnGUI(player, newX, newY, dir);
		});
	}

	private void sendChatMessage(String playerName, String message) {
		System.out.println("Sending chat message: " + message);
		String fullMessage = "CHAT " + playerName + ": " + message;
		sendToAll(fullMessage);
	}

	private void sendMove(String playerName, int x, int y, String direction) {
		String message = "MOVE " + playerName + " " + x + " " + y + " " + direction;
		sendToAll(message);
	}

	private void sendPointChange(String playerName, int points) {
		String message = "POINT " + playerName + " " + points;
		sendToAll(message);
	}

	private void sendToAll(String message) {
		System.out.println("I'm working");
		for (PrintWriter w : printers) {
			w.println(message);
			w.flush();
		}
	}
}

