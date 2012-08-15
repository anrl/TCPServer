package tcpserver.views;

import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import tcpserver.source.AudioChunk;
import tcpserver.source.KryoServer;
import tcpserver.source.Network;
import tcpserver.source.W12Server;

public class Main extends JFrame {
	final static float frequencies[] = {18604.6875f};
	
	private JPanel contentPane;
	private Clip clip;
	private AudioInputStream stream;
	private AudioFormat audioFormat;
	private AudioChunk chunk;

	JButton btnStartServer;
	JButton btnRunTests, btnSendSignal;
	JTextPane ipAddressText, totalClients, closestClient;
	KryoServer server;
	
	Thread w12ServerThread;
	W12Server w12Server;
	
	boolean serverOn = false;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Main() {
		prepareAudioSystem(44100, 16, 2);
		
		setTitle("TCPServer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 300, 240);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		
		btnStartServer = new JButton("Start Server");
		btnStartServer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
		    	if (!serverOn) {
		    		serverStart();
					clip.setFramePosition(0);
					clip.start();
					clip.loop(Clip.LOOP_CONTINUOUSLY);
		    	}
		    	else {
		    		serverStop();
		    		clip.stop();
		    	}
			}
		});
		
		btnRunTests = new JButton("Run Tests");
		btnRunTests.setEnabled(false);
		btnRunTests.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (serverOn) {
					server.sendToAll(100);
				}
			}
		});
		
		btnSendSignal = new JButton("Send Signal");
		btnSendSignal.setEnabled(false);
		btnSendSignal.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (serverOn) {
					server.sendBeepToClosest();
				}
			}
		});
		contentPane.setLayout(new MigLayout("", "[278px,grow]", "[25px][25px][][grow]"));
		contentPane.add(btnRunTests, "cell 0 1,growx,aligny top");
		contentPane.add(btnSendSignal, "cell 0 2,growx,aligny top");
		contentPane.add(btnStartServer, "cell 0 0,growx,aligny top");
		
		ipAddressText = new JTextPane();
		ipAddressText.setEditable(false);
		ipAddressText.setText(getLocalIpAddress());
		
		closestClient = new JTextPane();
		closestClient.setEditable(false);
		closestClient.setText("Closest Client: 0");
		
		totalClients = new JTextPane();
		totalClients.setEditable(false);
		totalClients.setText("Total Clients: 0");
		

		contentPane.add(ipAddressText, "cell 0 3,growx");
		contentPane.add(totalClients, "cell 0 4,growx");
		contentPane.add(closestClient, "cell 0 5,growx");
	}
	
	public void prepareAudioSystem(final int sampleRate, final int bitRate, final int channels) {
		try {
			clip = AudioSystem.getClip();
			audioFormat = new AudioFormat(sampleRate, bitRate, channels, true, true);
			
			chunk = new AudioChunk(audioFormat, 13120);
			chunk.appendSineWave(13120, frequencies, 31000);
			
			stream = new AudioInputStream(new ByteArrayInputStream(chunk.getData()), audioFormat, chunk.getLengthInBytes());
			
			clip.open(stream);
		} catch (LineUnavailableException e1) {
			System.err.println("The sound system is already in use by another application.");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
    
    public String getLocalIpAddress() {
    	URL ipURL;
    	String ipString = "IP: Offline";

    	try {
    		ipURL = new URL("http://myip.xname.org//");

    		BufferedReader in = new BufferedReader(
    				new InputStreamReader(ipURL.openStream()));

    		String inputLine;
    		while ((inputLine = in.readLine()) != null)
    			ipString = inputLine;
    		in.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	if (ipString.equals("IP: Offline"))
    		return ipString;
    	return "IP: " + ipString;
    }

	public void changeUI(final int status) {
		switch (status) {
		case Network.DISCONNECTED:
			btnRunTests.setEnabled(false);
			btnStartServer.setText("Start Server");
			totalClients.setText("Total Clients: 0");
			serverOn = false;
			break;
		case Network.CONNECTED:
			btnRunTests.setEnabled(true);
			btnStartServer.setText("Stop Server");
			serverOn = true;
			break;
		}
	}
	
    public void serverStart() {
    	w12Server = new W12Server(this);
    	w12ServerThread = new Thread(w12Server);
    	w12ServerThread.start();
    	System.out.println("Starting Server...");
    	server = new KryoServer(this);
    	server.bind();
    }
    
    public void serverStop() {
    	server.close();
    	w12Server.close();
    }
    
    public void receiveMessage(String msg) {
    	
    }
    
    public void setTotalClients(int value) {
		totalClients.setText("Total Clients: " + value);
    }
    
    public void setClosestClient(String id) {
    	closestClient.setText("Closest Client: " + id);
    }
}
