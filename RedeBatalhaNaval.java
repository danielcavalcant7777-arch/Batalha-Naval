import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;

public class RedeBatalhaNaval {
    private Socket socket;
    private ServerSocket serverSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean conectado = false;

    // Interface para avisar a tela (MenuBatalhaNaval) quando chegar uma mensagem
    public interface MensagemListener {
        void onMensagemRecebida(String mensagem);
        void onConexaoEstabelecida();
        void onErro(String erro);
    }

    private MensagemListener listener;

    public RedeBatalhaNaval(MensagemListener listener) {
        this.listener = listener;
    }

    // 1. MODO HOST (Cria a sala na porta especificada, ex: 12345)
    public void iniciarServidor(int porta) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(porta);
                socket = serverSocket.accept(); // Aguarda o amigo conectar
                configurarFluxos();
                SwingUtilities.invokeLater(() -> listener.onConexaoEstabelecida());
                escutarMensagens();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> listener.onErro("Erro no servidor: " + e.getMessage()));
            }
        }).start();
    }

    // 2. MODO CLIENTE (Conecta ao IP do PC Host)
    public void conectarCliente(String ipServidor, int porta) {
        new Thread(() -> {
            try {
                socket = new Socket(ipServidor, porta);
                configurarFluxos();
                SwingUtilities.invokeLater(() -> listener.onConexaoEstabelecida());
                escutarMensagens();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> listener.onErro("Erro ao conectar: " + e.getMessage()));
            }
        }).start();
    }

    private void configurarFluxos() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        conectado = true;
    }

    // Loop em background que fica ouvindo as jogadas do oponente
    private void escutarMensagens() {
        try {
            String linha;
            while (conectado && (linha = in.readLine()) != null) {
                final String msg = linha;
                // Garante que a atualização da tela ocorra na Thread do Swing
                SwingUtilities.invokeLater(() -> listener.onMensagemRecebida(msg));
            }
        } catch (IOException e) {
            if (conectado) {
                SwingUtilities.invokeLater(() -> listener.onErro("Conexão perdida com o oponente."));
            }
        }
    }

    public void enviarMensagem(String mensagem) {
        if (out != null && conectado) {
            out.println(mensagem);
        }
    }

    // Método útil para mostrar o IP local na tela do Host
    public static String getIpLocal() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    public void fechar() {
        conectado = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}