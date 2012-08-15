package tcpserver.source;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tcpserver.source.Network.DoBeep;
import tcpserver.source.Network.Signal;
import tcpserver.views.Main;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

public class KryoServer {
	private Main activity; //object to connect to the main activity
	private class Client {
		String androidID;
		Connection connection;
		int volume;
		long timems;
		
		public Client(String androidID, Connection connection, int volume, long timems) {
			this.androidID = androidID;
			this.connection = connection;
			this.volume = volume;
			this.timems = timems;
		}
	}
	
	Server server;
	List<Client> clientList = new ArrayList<Client>();
	
	public KryoServer(Main parent) {
    	activity = parent;
		server = new Server();
//		Log.TRACE();
		Network.register(server);
		
		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Signal) {
					Signal signal = (Signal)object;
					System.out.println("Received from '" + signal.id + "': " + signal.heard + ". Volume: " + signal.volume);
					boolean found = false;
					for (Client client: clientList) {
						if (client.connection.getID() == connection.getID()) {
							found = true;
							client.volume = signal.volume;
							client.timems = System.currentTimeMillis();
							break;
						}
					}
					if (!found) {
						clientList.add(new Client(signal.id, connection, signal.volume, System.currentTimeMillis()));
					}
					postClosestClient();
				}
			}

			public void disconnected(Connection connection) {
				for (Client client: clientList) {
					if (client.connection.getID() == connection.getID())
						clientList.remove(client);
				}
				postTotalClients();
			}

			public void connected(Connection connection) {
				postTotalClients();
			}
		});
		server.start();
	}
	
	public void close() {
		server.close();
		server.stop();
		postConnectionStatus(Network.DISCONNECTED);
	}
	
	public void bind() {
		try {
			server.bind(Network.TCPPort);
			postConnectionStatus(Network.CONNECTED);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server couldn't be opened.");
			postConnectionStatus(Network.DISCONNECTED);
		}
	}
	
	public void sendToAll(int volume) {
		Signal signal = new Signal();
		signal.volume = volume;
		signal.timems = System.currentTimeMillis();
		
		signal.heard = false;
		signal.id = "";
		
		server.sendToAllTCP(signal);
	}
	
	public void sendBeep(String id) {
		for (Client client: clientList) {
			if (client.androidID.equals(id)) {
				client.connection.sendTCP(new DoBeep());
			}
		}
	}
	
	public void sendBeepToClosest() {
		int lowest = 0;
		Connection lowestconnection = null;
		for (Client client: clientList) {
			System.out.println(client.volume);
			if (client.volume > lowest) {
				lowest = client.volume;
				lowestconnection = client.connection;
			}
		}
		if (lowestconnection != null)
			lowestconnection.sendTCP(new DoBeep());
	}

	private void postConnectionStatus(final int data) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				activity.changeUI(data);
			}
		});
    }

	private void postTotalClients() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				activity.setTotalClients(server.getConnections().length);
			}
		});
    }

	private void postClosestClient() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				int lowest = 0;
				String lowestid = "";
				for (Client client: clientList) {
					System.out.println(client.volume);
					if (client.volume > lowest) {
						lowest = client.volume;
						lowestid = client.androidID;
					}
				}
				activity.setClosestClient(lowestid);
			}
		});
    }
}
