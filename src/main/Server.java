package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.GregorianCalendar;

public class Server {

	public static void main(String[] args) throws IOException {

		ServerSocket serverSocket = new ServerSocket(11253);

		while (true) {
			System.out.println("Cekam novog klijenta");
			Socket communicationSocket = serverSocket.accept();
			Thread thread = new Thread(new ClientThread(communicationSocket));
			// System.out.println("konekcija za thread: " + thread.getId());
			thread.start();
		}

	}

	private static class ClientThread implements Runnable { // deo za paralelno izvrsavanje
		private Socket socket;

		public ClientThread(Socket a) {
			this.socket = a;
		}

		@Override
		public void run() {
			BufferedReader clientInputStream;
			PrintStream clientOutputStream;
			int serverRequestCode = 0;

			try { // ovo deluje ekstremno glupo; medjutim radi
				clientInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream())); // stream init
				clientOutputStream = new PrintStream(socket.getOutputStream());
				while (true) {
					serverRequestCode = Integer.parseInt(clientInputStream.readLine());
					switch (serverRequestCode) {
					case 0:
						System.out.println("korisnik terminirao sesiju");
						return;
					case 1:
						// System.out.println("korisnik vrsi uplatu");
						izvrsiUplatu(clientInputStream, clientOutputStream);
						break;
					case 2:
						prikaziStanje(clientOutputStream);
						break;
					case 3:
						registracija(clientInputStream, clientOutputStream);
						break;
					case 4:
						login(clientInputStream, clientOutputStream);
						break;
					case 5:
						uplate(clientInputStream, clientOutputStream);
						break;
					case 6:
						// logout
						break;
					default:
						System.out.println("korisnik izabrao nevazecu opciju");
						break;
					}
				}

			} catch (IOException e) {
				System.out.println("io err runnable");
				e.printStackTrace();
			}

		}

		private void izvrsiUplatu(BufferedReader in, PrintStream out) throws IOException {
			BufferedReader file = new BufferedReader(new FileReader("./baza/kartice.txt"));
			GregorianCalendar datum;
			String datumStr;
			String[] unos = in.readLine().split("!"); // ime prezime adresa iznos kartica cvv
			for (String buffer = file.readLine(); buffer != null; buffer = file.readLine()) {
				if (unos[4].equals(buffer)) {
					buffer = file.readLine();
					if (unos[5].equals(buffer)) {
						System.out.println("uplata validirana");
						out.println("OK");
						file.close();
						datum = new GregorianCalendar();
						datumStr = datum.get(GregorianCalendar.DAY_OF_MONTH) + "." + (datum.get(GregorianCalendar.MONTH) + 1) + "." + datum.get(GregorianCalendar.YEAR) + ". " + datum.get(GregorianCalendar.HOUR) + ":" + datum.get(GregorianCalendar.MINUTE);
						out.println(datumStr);
						file = new BufferedReader(new FileReader("./baza/pare.txt"));
						int suma = Integer.parseInt(file.readLine());
						file.close();
						file = new BufferedReader(new FileReader("./baza/uplate.txt"));
						String logEntry = unos[0] + " " + unos[1] + " " + datumStr + " " + unos[3] + "rsd";
						String log = logEntry;
						for (int i = 0; i < 9; i++) {
							log = log + "\n" + file.readLine();
						}
						file.close();
						BufferedWriter fileWriter = new BufferedWriter(new FileWriter("./baza/uplate.txt"));
						fileWriter.write(log);
						fileWriter.close();
						fileWriter = new BufferedWriter(new FileWriter("./baza/pare.txt"));
						suma += Integer.parseInt(unos[3]);
						fileWriter.write(Integer.toString(suma));
						fileWriter.close();
						return;
					}
				}
			}
			out.println("ERR");
			file.close();

		}

		private void prikaziStanje(PrintStream out) throws IOException {
			BufferedReader file = new BufferedReader(new FileReader("./baza/pare.txt"));
			out.println(file.readLine());
			file.close();
		}

		private void registracija(BufferedReader in, PrintStream out) throws IOException {
			String user, buffer;
			user = in.readLine(); // citanje kartice
			BufferedReader fileKartice = new BufferedReader(new FileReader("./baza/kartice.txt"));
			buffer = fileKartice.readLine();
			while (buffer != null) {
				if (user.equals(buffer)) { // uspeh
					// System.out.println("kartica vazeca");
					user = null;
					break;
				}
				buffer = fileKartice.readLine();
			}
			if (user != null) {
				out.println("ERR");
				fileKartice.close();
				return;
			}
			out.println("OK");
			BufferedReader file = new BufferedReader(new FileReader("./baza/user.txt"));
			user = in.readLine(); // citanje celokupnih unetih podataka
			buffer = file.readLine();
			while (buffer != null) {
				if (buffer.split(";")[0].equals(user.split(";")[0])) {
					out.println("ERR");
					file.close();
					fileKartice.close();
					return;
				}
				buffer = file.readLine();
			}
			file.close();
			fileKartice.close();
			out.println("OK");
			BufferedWriter fileWriter = new BufferedWriter(new FileWriter("./baza/user.txt", true)); // append == true
			fileWriter.write("\n" + user);
			fileWriter.close();
		}

		private void login(BufferedReader in, PrintStream out) throws IOException {
			BufferedReader file = new BufferedReader(new FileReader("./baza/user.txt"));
			String buffer = file.readLine();
			String userdata = in.readLine();
			while (buffer != null) {
				if (buffer.split(";")[0].equals(userdata.split(";")[0])) { // nisam ponosan na ovo
					if (buffer.split(";")[1].equals(userdata.split(";")[1])) {
						out.println("OK");
						out.println(buffer);
						file.close();
						return;
					}
				}
				buffer = file.readLine();
			}
			out.println("ERR");
			file.close();
		}

		private void uplate(BufferedReader in, PrintStream out) throws IOException {
			if (in.readLine().equals("OK") == false) {
				return;
			}
			BufferedReader fileUplate = new BufferedReader(new FileReader("./baza/uplate.txt"));
			for (int i = 0; i < 10; i++) {
				out.println(fileUplate.readLine());
			}
			fileUplate.close();
			return;
		}

	}

}
