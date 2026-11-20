import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MenuBatalhaNaval extends JPanel implements RedeBatalhaNaval.MensagemListener {

    // ===== Paleta de cores =====
    private static final Color AZUL_TOPO     = new Color(10, 40, 90);
    private static final Color AZUL_FUNDO    = new Color(30, 110, 170);
    private static final Color CINZA_METAL_1 = new Color(200, 205, 212);
    private static final Color CINZA_METAL_3 = new Color(60, 66, 74);
    private static final Color VERDE_BASE    = new Color(51, 82, 36);
    private static final Color VERDE_CLARO   = new Color(78, 110, 56);
    private static final Color VERDE_SOMBRA  = new Color(28, 45, 20);
    private static final Color CONTORNO      = new Color(15, 25, 10);
    private static final Color TEXTO_BOTAO   = new Color(215, 220, 190);
    private static final Color AVISO         = new Color(255, 130, 110);

    private static final Color AGUA          = new Color(20, 80, 130);
    private static final Color AGUA_CLARA    = new Color(40, 110, 165);
    private static final Color CASCO         = new Color(150, 156, 164);
    private static final Color CASCO_ESCURO  = new Color(90, 96, 104);
    private static final Color AFUNDADO      = new Color(85, 88, 94);
    private static final Color AFUNDADO_ESC  = new Color(50, 52, 56);
    private static final Color ACERTO        = new Color(200, 40, 40);
    private static final Color ERRO          = new Color(230, 230, 230);
    private static final Color PREVIA_OK     = new Color(120, 220, 120, 130);
    private static final Color PREVIA_RUIM   = new Color(230, 60, 60, 130);

    private static final int PIXEL = 6;
    private static final int BOTAO_LARGURA = 380;
    private static final int BOTAO_ALTURA = 56;
    private static final int MAX_NOME = 12;
    private static final int PORTA_REDE = 12345;

    // ===== Configuracao por dificuldade =====
    private static final int TAM_FACIL = 6;
    private static final int TAM_DIFICIL = 8;
    private static final int[] FROTA_FACIL   = {3, 2, 1, 1};
    private static final int[] FROTA_DIFICIL = {4, 3, 2, 2, 1};

    // ===== Sprites dos navios (pasta assets/, ao lado dos .java) =====
    private static final String PASTA_SPRITES = "assets";
    private static final Map<Integer, BufferedImage> SPRITES = new HashMap<>();          // horizontal
    private static final Map<Integer, BufferedImage> SPRITES_VERTICAIS = new HashMap<>(); // girados
    static {
        // Tamanho do navio -> sprite
        carregarSprite(4, "encouracado.png");
        carregarSprite(3, "cruzador.png");
        carregarSprite(2, "contratorpedeiro.png");
        carregarSprite(1, "submarino.png");
        for (Map.Entry<Integer, BufferedImage> e : SPRITES.entrySet()) {
            SPRITES_VERTICAIS.put(e.getKey(), girarHorario(e.getValue()));
        }
    }

    private static void carregarSprite(int tamanho, String arquivo) {
        try {
            BufferedImage img = ImageIO.read(new File(PASTA_SPRITES, arquivo));
            if (img != null) SPRITES.put(tamanho, img);
        } catch (IOException ignorado) {
            // Sem sprite: o navio continua sendo desenhado com retangulos.
        }
    }

    private static BufferedImage girarHorario(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(h - 1 - y, x, img.getRGB(x, y));
        return out;
    }

    private enum Tela {
        MENU, OPCOES, MODO_JOGO, DIFICULDADE, NOMES,
        HOST_ESPERA, CLIENTE_CONECTAR, POSICIONAR, TRANSICAO, BATALHA, FIM
    }

    private final JFrame janela;
    private final Random random = new Random();
    private Tela tela = Tela.MENU;
    private boolean telaCheia = false;
    private Botao[] botoes;
    private int selecionado = 0;

    private String dificuldade = "FACIL";
    private final String[] nomes = {"", ""};

    // ===== Modos de Jogo =====
    private boolean modoIA = false;
    private IaBatalhaNaval ia;
    private Timer timerIA;

    // ===== Estado Multiplayer Rede =====
    private RedeBatalhaNaval rede;
    private boolean modoRede = false;
    private boolean souHost = false;
    private int meuIndice = 0;
    private String ipDigitado = "";
    private String statusRede = "";
    private boolean euPronto = false;
    private boolean oponentePronto = false;
    private boolean aguardandoRespostaTiro = false;

    // Estado da partida
    private Jogo jogo;
    private int jogadorPosicionando = 0;
    private boolean horizontal = true;
    private int cursorL = 0, cursorC = 0;
    private boolean aguardandoContinuar = false;
    private Tabuleiro.ResultadoTiro ultimoResultado;
    private String aviso = "";

    // Tela de transicao ("passe para o outro jogador")
    private String textoTransicao = "";
    private Runnable aposTransicao;

    // Area do tabuleiro clicavel
    private boolean tabAtivo = false;
    private int tabX, tabY, tabCel, tabN;

    public MenuBatalhaNaval(JFrame janela) {
        this.janela = janela;
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        montarMenu();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int[] cel = celulaEm(e.getX(), e.getY());
                if (cel != null) {
                    cursorL = cel[0];
                    cursorC = cel[1];
                }
                int sobre = botaoEm(e.getX(), e.getY());
                if (telaDeTabuleiro()) {
                    selecionado = sobre;
                } else if (sobre >= 0) {
                    selecionado = sobre;
                }
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (tela == Tela.POSICIONAR && !euPronto && SwingUtilities.isRightMouseButton(e)) {
                    horizontal = !horizontal;
                    repaint();
                    return;
                }
                int[] cel = celulaEm(e.getX(), e.getY());
                if (cel != null) {
                    cursorL = cel[0];
                    cursorC = cel[1];
                    acaoNaCelula(cel[0], cel[1]);
                    repaint();
                    return;
                }
                int i = botaoEm(e.getX(), e.getY());
                if (i >= 0) {
                    if ((tela == Tela.NOMES || tela == Tela.CLIENTE_CONECTAR) && botoes[i].campo) {
                        selecionado = i;
                    } else {
                        acionar(botoes[i].indice);
                    }
                }
                repaint();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (tela) {
                    case POSICIONAR: teclaPosicionar(e); break;
                    case BATALHA:    teclaBatalha(e);    break;
                    default:         teclaMenu(e);       break;
                }
                repaint();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (tela == Tela.NOMES) {
                    int maxCampos = (modoRede || modoIA) ? 0 : 1;
                    if (selecionado < 0 || selecionado > maxCampos) return;
                    int idxNome = modoRede ? meuIndice : selecionado;
                    editarTexto(e.getKeyChar(), idxNome);
                } else if (tela == Tela.CLIENTE_CONECTAR) {
                    if (selecionado == 0) {
                        editarTexto(e.getKeyChar(), 1);
                    } else if (selecionado == 1) {
                        editarIp(e.getKeyChar());
                    }
                }
                repaint();
            }
        });
    }

    private void editarTexto(char c, int idxNome) {
        if (c == '\b') {
            if (!nomes[idxNome].isEmpty()) {
                nomes[idxNome] = nomes[idxNome].substring(0, nomes[idxNome].length() - 1);
            }
        } else {
            String limpo = normalizar(String.valueOf(c)).toUpperCase();
            for (char ch : limpo.toCharArray()) {
                boolean valido = (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == ' ';
                if (ch == ' ' && nomes[idxNome].isEmpty()) valido = false;
                if (valido && nomes[idxNome].length() < MAX_NOME) {
                    nomes[idxNome] += ch;
                }
            }
        }
    }

    private void editarIp(char c) {
        if (c == '\b') {
            if (!ipDigitado.isEmpty()) {
                ipDigitado = ipDigitado.substring(0, ipDigitado.length() - 1);
            }
        } else if (((c >= '0' && c <= '9') || c == '.') && ipDigitado.length() < 15) {
            ipDigitado += c;
        }
    }

    private boolean telaDeTabuleiro() {
        return tela == Tela.POSICIONAR || tela == Tela.BATALHA;
    }

    // ===== Teclado =====
    private void teclaMenu(KeyEvent e) {
        if (botoes.length == 0) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                selecionado = selecionado < 0 ? 0 : (selecionado - 1 + botoes.length) % botoes.length;
                break;
            case KeyEvent.VK_DOWN:
                selecionado = selecionado < 0 ? 0 : (selecionado + 1) % botoes.length;
                break;
            case KeyEvent.VK_ENTER:
                if (selecionado >= 0) acionar(botoes[selecionado].indice);
                break;
            case KeyEvent.VK_ESCAPE:
                voltarTela();
                break;
        }
    }

    private void teclaPosicionar(KeyEvent e) {
        if (euPronto) return;
        int n = jogo.tabuleiro(jogadorPosicionando).tamanho();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    cursorL = Math.max(0, cursorL - 1); break;
            case KeyEvent.VK_DOWN:  cursorL = Math.min(n - 1, cursorL + 1); break;
            case KeyEvent.VK_LEFT:  cursorC = Math.max(0, cursorC - 1); break;
            case KeyEvent.VK_RIGHT: cursorC = Math.min(n - 1, cursorC + 1); break;
            case KeyEvent.VK_R:     horizontal = !horizontal; break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER: cliqueNaCelula(cursorL, cursorC); break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                jogo.tabuleiro(jogadorPosicionando).remover(
                    jogo.tabuleiro(jogadorPosicionando).navioEm(cursorL, cursorC));
                break;
            case KeyEvent.VK_A:     posicionarAleatorio(); break;
            case KeyEvent.VK_C:     confirmarPosicionamento(); break;
            case KeyEvent.VK_ESCAPE: voltarTela(); break;
        }
    }

    private void teclaBatalha(KeyEvent e) {
        int n = jogo.tabuleiro(1 - meuIndice).tamanho();
        int k = e.getKeyCode();
        if (aguardandoContinuar && !modoRede && !modoIA) {
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) continuar();
            return;
        }
        switch (k) {
            case KeyEvent.VK_UP:    cursorL = Math.max(0, cursorL - 1); break;
            case KeyEvent.VK_DOWN:  cursorL = Math.min(n - 1, cursorL + 1); break;
            case KeyEvent.VK_LEFT:  cursorC = Math.max(0, cursorC - 1); break;
            case KeyEvent.VK_RIGHT: cursorC = Math.min(n - 1, cursorC + 1); break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER: atirar(cursorL, cursorC); break;
        }
    }

    // ===== Troca de telas =====
    private void encerrarModosAtivos() {
        if (timerIA != null) {
            timerIA.stop();
            timerIA = null;
        }
        if (rede != null) {
            rede.fechar();
            rede = null;
        }
        modoIA = false;
        modoRede = false;
        souHost = false;
        euPronto = false;
        oponentePronto = false;
        aguardandoRespostaTiro = false;
    }

    private void montarMenu() {
        encerrarModosAtivos();
        tela = Tela.MENU;
        selecionado = 0;
        botoes = new Botao[] {
            new Botao("JOGAR",  0),
            new Botao("OPCOES", 1),
            new Botao("SAIR",   2)
        };
    }

    private void montarOpcoes() {
        tela = Tela.OPCOES;
        selecionado = 0;
        botoes = new Botao[] {
            new Botao(rotuloTelaCheia(), 0),
            new Botao("VOLTAR", 1)
        };
    }

    private void montarModoJogo() {
        encerrarModosAtivos();
        tela = Tela.MODO_JOGO;
        selecionado = 0;
        botoes = new Botao[] {
            new Botao("CONTRA IA",        0),
            new Botao("MESMO PC",         1),
            new Botao("CRIAR SALA (LAN)", 2),
            new Botao("ENTRAR NA SALA",   3),
            new Botao("VOLTAR",           4)
        };
    }

    private void montarDificuldade() {
        tela = Tela.DIFICULDADE;
        selecionado = 0;
        botoes = new Botao[] {
            new Botao("FACIL",   0),
            new Botao("DIFICIL", 1),
            new Botao("VOLTAR",  2)
        };
    }

    private void montarNomes() {
        tela = Tela.NOMES;
        selecionado = 0;
        if (modoRede) {
            botoes = new Botao[] {
                new Botao("", 0, true),
                new Botao("CRIAR SALA", 2),
                new Botao("VOLTAR", 3)
            };
        } else if (modoIA) {
            botoes = new Botao[] {
                new Botao("", 0, true),
                new Botao("CONFIRMAR", 2),
                new Botao("VOLTAR", 3)
            };
        } else {
            botoes = new Botao[] {
                new Botao("", 0, true),
                new Botao("", 1, true),
                new Botao("CONFIRMAR", 2),
                new Botao("VOLTAR", 3)
            };
        }
    }

    private void montarHostEspera() {
        tela = Tela.HOST_ESPERA;
        statusRede = "AGUARDANDO AMIGO CONECTAR...";
        selecionado = 0;
        botoes = new Botao[] {
            new Botao("CANCELAR", 0)
        };
        rede = new RedeBatalhaNaval(this);
        rede.iniciarServidor(PORTA_REDE);
    }

    private void montarClienteConectar() {
        tela = Tela.CLIENTE_CONECTAR;
        statusRede = "";
        selecionado = 0;
        botoes = new Botao[] {
            new Botao("", 0, true),
            new Botao("", 1, true),
            new Botao("CONECTAR", 2),
            new Botao("VOLTAR", 3)
        };
    }

    private void montarPosicionar(int jogador) {
        tela = Tela.POSICIONAR;
        jogadorPosicionando = jogador;
        jogo.tabuleiro(jogador).limpar();
        horizontal = true;
        cursorL = 0;
        cursorC = 0;
        aviso = "";
        euPronto = false;
        botoes = new Botao[] {
            new Botao("ALEATORIO", 0),
            new Botao("CONFIRMAR", 1),
            new Botao("VOLTAR", 2)
        };
        selecionado = -1;
    }

    private void mostrarTransicao(String nome, Runnable depois) {
        tela = Tela.TRANSICAO;
        textoTransicao = nome;
        aposTransicao = depois;
        botoes = new Botao[] { new Botao("PRONTO", 0) };
        selecionado = 0;
    }

    private void montarBatalha() {
        tela = Tela.BATALHA;
        aguardandoContinuar = false;
        aguardandoRespostaTiro = false;
        ultimoResultado = null;
        aviso = "";
        cursorL = 0;
        cursorC = 0;
        botoes = new Botao[0];
        selecionado = -1;
    }

    private void montarFim() {
        if (timerIA != null) timerIA.stop();
        tela = Tela.FIM;
        if (modoRede) {
            botoes = new Botao[] {
                new Botao("REVANCHE", 0),
                new Botao("MENU", 1)
            };
        } else {
            botoes = new Botao[] {
                new Botao("JOGAR NOVAMENTE", 0),
                new Botao("MENU", 1)
            };
        }
        selecionado = 0;
    }

    private void voltarTela() {
        switch (tela) {
            case OPCOES:      montarMenu(); break;
            case MODO_JOGO:   montarMenu(); break;
            case DIFICULDADE: montarModoJogo(); break;
            case NOMES:       montarDificuldade(); break;
            case HOST_ESPERA:
            case CLIENTE_CONECTAR:
                encerrarModosAtivos();
                montarModoJogo();
                break;
            case POSICIONAR:
                if (modoRede) {
                    montarMenu();
                } else if (modoIA || jogadorPosicionando == 0) {
                    montarNomes();
                } else {
                    montarPosicionar(0);
                }
                break;
            default: break;
        }
    }

    private String rotuloTelaCheia() {
        return "TELA CHEIA: " + (telaCheia ? "ON" : "OFF");
    }

    private void acionar(int indice) {
        switch (tela) {
            case MENU:
                switch (indice) {
                    case 0: montarModoJogo(); break;
                    case 1: montarOpcoes(); break;
                    case 2: System.exit(0); break;
                }
                break;

            case OPCOES:
                switch (indice) {
                    case 0: alternarTelaCheia(); break;
                    case 1: montarMenu(); break;
                }
                break;

            case MODO_JOGO:
                switch (indice) {
                    case 0:
                        modoIA = true;
                        meuIndice = 0;
                        montarDificuldade();
                        break;
                    case 1:
                        meuIndice = 0;
                        montarDificuldade();
                        break;
                    case 2:
                        modoRede = true;
                        souHost = true;
                        meuIndice = 0;
                        montarDificuldade();
                        break;
                    case 3:
                        modoRede = true;
                        souHost = false;
                        meuIndice = 1;
                        montarClienteConectar();
                        break;
                    case 4:
                        montarMenu();
                        break;
                }
                break;

            case DIFICULDADE:
                switch (indice) {
                    case 0: dificuldade = "FACIL";   montarNomes(); break;
                    case 1: dificuldade = "DIFICIL"; montarNomes(); break;
                    case 2: montarModoJogo(); break;
                }
                break;

            case NOMES:
                if (modoRede) {
                    switch (indice) {
                        case 0: selecionado = 1; break;
                        case 2: montarHostEspera(); break;
                        case 3: montarDificuldade(); break;
                    }
                } else if (modoIA) {
                    switch (indice) {
                        case 0: selecionado = 1; break;
                        case 2: iniciarPartidaIA(); break;
                        case 3: montarDificuldade(); break;
                    }
                } else {
                    switch (indice) {
                        case 0: selecionado = 1; break;
                        case 1: selecionado = 2; break;
                        case 2: iniciarPartidaLocal(); break;
                        case 3: montarDificuldade(); break;
                    }
                }
                break;

            case HOST_ESPERA:
                voltarTela();
                break;

            case CLIENTE_CONECTAR:
                switch (indice) {
                    case 0: selecionado = 1; break;
                    case 1: selecionado = 2; break;
                    case 2: conectarAoHost(); break;
                    case 3: voltarTela(); break;
                }
                break;

            case POSICIONAR:
                if (euPronto && indice != 2) return;
                switch (indice) {
                    case 0: posicionarAleatorio(); break;
                    case 1: confirmarPosicionamento(); break;
                    case 2: voltarTela(); break;
                }
                break;

            case TRANSICAO:
                if (aposTransicao != null) aposTransicao.run();
                break;

            case BATALHA:
                if (indice == 0 && aguardandoContinuar && !modoRede && !modoIA) continuar();
                break;

            case FIM:
                switch (indice) {
                    case 0:
                        if (modoRede) {
                            reiniciarPartidaRede();
                            rede.enviarMensagem("REINICIAR");
                        } else if (modoIA) {
                            iniciarPartidaIA();
                        } else {
                            iniciarPartidaLocal();
                        }
                        break;
                    case 1:
                        montarMenu();
                        break;
                }
                break;
        }
        repaint();
    }

    // ===== Fluxo Rede =====
    private void conectarAoHost() {
        String ip = ipDigitado.trim();
        if (ip.isEmpty()) {
            statusRede = "DIGITE O IP DO HOST!";
            return;
        }
        statusRede = "CONECTANDO EM " + ip + "...";
        if (rede != null) rede.fechar();
        rede = new RedeBatalhaNaval(this);
        rede.conectarCliente(ip, PORTA_REDE);
    }

    @Override
    public void onConexaoEstabelecida() {
        if (souHost) {
            rede.enviarMensagem("CONFIG:" + dificuldade + ":" + nomeOuPadrao(0));
        } else {
            rede.enviarMensagem("OLA:" + nomeOuPadrao(1));
            statusRede = "CONECTADO! AGUARDANDO HOST...";
        }
        repaint();
    }

    @Override
    public void onMensagemRecebida(String msg) {
        if (msg.startsWith("CONFIG:")) {
            String[] p = msg.split(":", 3);
            dificuldade = p[1];
            nomes[0] = p[2];
            iniciarPartidaRede();
        }
        else if (msg.startsWith("OLA:")) {
            nomes[1] = msg.split(":", 2)[1];
            iniciarPartidaRede();
        }
        else if (msg.equals("PRONTO")) {
            oponentePronto = true;
            if (euPronto) {
                jogo.iniciarBatalhaRede();
                montarBatalha();
            } else {
                aviso = "OPONENTE JA POSICIONOU!";
            }
        }
        else if (msg.startsWith("TIRO:")) {
            String[] lc = msg.split(":")[1].split(",");
            int l = Integer.parseInt(lc[0]);
            int c = Integer.parseInt(lc[1]);

            Tabuleiro meuTab = jogo.tabuleiro(meuIndice);
            int idNavio = meuTab.navioEm(l, c);
            Tabuleiro.ResultadoTiro r = meuTab.atirar(l, c);
            String coordsAfundado = (r == Tabuleiro.ResultadoTiro.AFUNDOU) ? meuTab.celulasDoNavio(idNavio) : "";
            boolean fim = meuTab.todosAfundados();

            jogo.registrarTiroRede(1 - meuIndice, l, c, r, idNavio, coordsAfundado, fim);
            rede.enviarMensagem("RES:" + l + "," + c + "," + r.name() + "," + idNavio + "," + fim + "," + coordsAfundado);

            if (fim) {
                montarFim();
            } else {
                jogo.passarVez();
                aviso = r == Tabuleiro.ResultadoTiro.AGUA ? "OPONENTE ERROU! SUA VEZ" : "OPONENTE ACERTOU! SUA VEZ";
            }
        }
        else if (msg.startsWith("RES:")) {
            String[] p = msg.substring(4).split(",", 6);
            int l = Integer.parseInt(p[0]);
            int c = Integer.parseInt(p[1]);
            Tabuleiro.ResultadoTiro r = Tabuleiro.ResultadoTiro.valueOf(p[2]);
            int idNavio = Integer.parseInt(p[3]);
            boolean fim = Boolean.parseBoolean(p[4]);
            String coordsAfundado = p.length > 5 ? p[5] : "";

            jogo.registrarTiroRede(meuIndice, l, c, r, idNavio, coordsAfundado, fim);
            aguardandoRespostaTiro = false;

            if (fim) {
                montarFim();
            } else {
                jogo.passarVez();
                switch (r) {
                    case AGUA:    aviso = "AGUA! VEZ DO OPONENTE"; break;
                    case ACERTO:  aviso = "ACERTOU! VEZ DO OPONENTE"; break;
                    case AFUNDOU: aviso = "AFUNDOU UM NAVIO! VEZ DO OPONENTE"; break;
                    default: break;
                }
            }
        }
        else if (msg.equals("REINICIAR")) {
            reiniciarPartidaRede();
        }
        repaint();
    }

    @Override
    public void onErro(String erro) {
        if (tela == Tela.CLIENTE_CONECTAR || tela == Tela.HOST_ESPERA) {
            statusRede = "FALHA NA CONEXAO!";
        } else {
            JOptionPane.showMessageDialog(this, erro, "Conexao Encerrada", JOptionPane.WARNING_MESSAGE);
            montarMenu();
        }
        repaint();
    }

    // ===== Fluxo da partida =====
    private String nomeOuPadrao(int i) {
        String n = nomes[i].trim();
        return n.isEmpty() ? "JOGADOR " + (i + 1) : n;
    }

    private void iniciarPartidaIA() {
        boolean facil = dificuldade.equals("FACIL");
        ia = new IaBatalhaNaval(!facil); // No Dificil a IA persegue navios atingidos
        jogo = new Jogo(nomeOuPadrao(0), "CPU IA",
                        facil ? TAM_FACIL : TAM_DIFICIL,
                        facil ? FROTA_FACIL : FROTA_DIFICIL);
        jogo.tabuleiro(1).posicionarAleatorio(random);
        montarPosicionar(0);
    }

    private void iniciarPartidaLocal() {
        boolean facil = dificuldade.equals("FACIL");
        jogo = new Jogo(nomeOuPadrao(0), nomeOuPadrao(1),
                        facil ? TAM_FACIL : TAM_DIFICIL,
                        facil ? FROTA_FACIL : FROTA_DIFICIL);
        montarPosicionar(0);
    }

    private void iniciarPartidaRede() {
        boolean facil = dificuldade.equals("FACIL");
        euPronto = false;
        oponentePronto = false;
        jogo = new Jogo(nomeOuPadrao(0), nomeOuPadrao(1),
                        facil ? TAM_FACIL : TAM_DIFICIL,
                        facil ? FROTA_FACIL : FROTA_DIFICIL);
        montarPosicionar(meuIndice);
    }

    private void reiniciarPartidaRede() {
        iniciarPartidaRede();
        repaint();
    }

    private void acaoNaCelula(int l, int c) {
        if (tela == Tela.POSICIONAR) cliqueNaCelula(l, c);
        else if (tela == Tela.BATALHA) atirar(l, c);
    }

    private void cliqueNaCelula(int l, int c) {
        if (euPronto) return;
        Tabuleiro t = jogo.tabuleiro(jogadorPosicionando);
        int id = t.navioEm(l, c);
        if (id >= 0) {
            t.remover(id);
            aviso = "";
            return;
        }
        int proximo = t.proximoNaoColocado();
        if (proximo < 0) {
            aviso = "TODOS OS NAVIOS JA FORAM COLOCADOS";
            return;
        }
        aviso = t.colocar(proximo, l, c, horizontal) ? "" : "NAO CABE AI!";
    }

    private void posicionarAleatorio() {
        if (euPronto) return;
        jogo.tabuleiro(jogadorPosicionando).posicionarAleatorio(random);
        aviso = "";
    }

    private void confirmarPosicionamento() {
        if (!jogo.tabuleiro(jogadorPosicionando).todosColocados()) {
            aviso = "COLOQUE TODOS OS NAVIOS!";
            return;
        }
        if (modoRede) {
            euPronto = true;
            rede.enviarMensagem("PRONTO");
            if (oponentePronto) {
                jogo.iniciarBatalhaRede();
                montarBatalha();
            } else {
                aviso = "AGUARDANDO OPONENTE...";
            }
        } else if (modoIA) {
            jogo.iniciarBatalha();
            montarBatalha();
        } else {
            if (jogadorPosicionando == 0) {
                mostrarTransicao(jogo.nome(1), () -> montarPosicionar(1));
            } else {
                jogo.iniciarBatalha();
                mostrarTransicao(jogo.nome(0), this::montarBatalha);
            }
        }
    }

    private void atirar(int l, int c) {
        if (modoRede) {
            if (jogo.vez() != meuIndice || aguardandoRespostaTiro) return;
            Tabuleiro alvo = jogo.tabuleiro(1 - meuIndice);
            if (!alvo.dentro(l, c)) return;
            if (alvo.foiAtingida(l, c)) {
                aviso = "VOCE JA ATIROU AI!";
                return;
            }
            aguardandoRespostaTiro = true;
            aviso = "DISPARANDO...";
            rede.enviarMensagem("TIRO:" + l + "," + c);
            return;
        }

        if (modoIA) {
            if (jogo.vez() != 0 || aguardandoRespostaTiro) return;
            Tabuleiro.ResultadoTiro r = jogo.atirar(l, c);
            switch (r) {
                case REPETIDO: aviso = "VOCE JA ATIROU AI!"; return;
                case INVALIDO: return;
                case AGUA:     aviso = "AGUA! VEZ DA IA"; break;
                case ACERTO:   aviso = "ACERTOU! VEZ DA IA"; break;
                case AFUNDOU:  aviso = "AFUNDOU UM NAVIO! VEZ DA IA"; break;
            }
            if (jogo.fase() == Jogo.Fase.FIM) {
                montarFim();
                return;
            }
            jogo.passarVez();
            aguardandoRespostaTiro = true;

            // Pequeno atraso para o jogador ver seu tiro antes da IA responder
            timerIA = new Timer(700, e -> executarTurnoIA());
            timerIA.setRepeats(false);
            timerIA.start();
            return;
        }

        // Modo local (mesmo PC)
        if (aguardandoContinuar) return;
        Tabuleiro.ResultadoTiro r = jogo.atirar(l, c);
        switch (r) {
            case REPETIDO: aviso = "VOCE JA ATIROU AI!"; return;
            case INVALIDO: return;
            case AGUA:     aviso = "AGUA!"; break;
            case ACERTO:   aviso = "ACERTOU!"; break;
            case AFUNDOU:  aviso = "AFUNDOU UM NAVIO!"; break;
        }
        ultimoResultado = r;
        aguardandoContinuar = true;
        botoes = new Botao[] { new Botao("CONTINUAR", 0) };
        selecionado = 0;
    }

    private void executarTurnoIA() {
        if (tela != Tela.BATALHA || jogo.fase() != Jogo.Fase.BATALHA) return;
        int[] coord = ia.escolherTiro(jogo.tabuleiro(0));
        Tabuleiro.ResultadoTiro r = jogo.atirar(coord[0], coord[1]);
        ia.registrarResultado(coord[0], coord[1], r);
        aguardandoRespostaTiro = false;

        if (jogo.fase() == Jogo.Fase.FIM) {
            montarFim();
        } else {
            jogo.passarVez();
            switch (r) {
                case AGUA:    aviso = "IA ERROU! SUA VEZ"; break;
                case ACERTO:  aviso = "IA ACERTOU SEU NAVIO! SUA VEZ"; break;
                case AFUNDOU: aviso = "IA AFUNDOU SEU NAVIO! SUA VEZ"; break;
                default: break;
            }
        }
        repaint();
    }

    private void continuar() {
        if (jogo.fase() == Jogo.Fase.FIM) {
            montarFim();
        } else {
            jogo.passarVez();
            mostrarTransicao(jogo.nome(jogo.vez()), this::montarBatalha);
        }
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    // ===== Coordenadas =====
    private int[] celulaEm(int px, int py) {
        if (!tabAtivo || px < tabX || py < tabY) return null;
        int c = (px - tabX) / tabCel;
        int l = (py - tabY) / tabCel;
        if (c >= tabN || l >= tabN) return null;
        return new int[] {l, c};
    }

    private int botaoEm(int px, int py) {
        for (int i = 0; i < botoes.length; i++) {
            if (botoes[i].contem(px, py)) return i;
        }
        return -1;
    }

    // ===== Tela cheia =====
    private void alternarTelaCheia() {
        telaCheia = !telaCheia;
        GraphicsDevice dispositivo = janela.getGraphicsConfiguration().getDevice();

        if (telaCheia) {
            if (dispositivo.isFullScreenSupported()) {
                dispositivo.setFullScreenWindow(janela);
            } else {
                janela.setExtendedState(Frame.MAXIMIZED_BOTH);
            }
        } else {
            dispositivo.setFullScreenWindow(null);
            janela.setExtendedState(Frame.NORMAL);
            janela.pack();
            janela.setLocationRelativeTo(null);
        }

        botoes[0].rotulo = rotuloTelaCheia();
        requestFocusInWindow();
    }

    // ===== Desenho =====
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int w = getWidth();
        int h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, AZUL_TOPO, 0, h, AZUL_FUNDO));
        g2.fillRect(0, 0, w, h);
        desenharOndas(g2, w, h);

        tabAtivo = false;
        switch (tela) {
            case POSICIONAR: desenharPosicionar(g2, w, h); break;
            case BATALHA:    desenharBatalha(g2, w, h);    break;
            default:         desenharTelaMenu(g2, w, h);   break;
        }
        g2.dispose();
    }

    private void desenharTelaMenu(Graphics2D g2, int w, int h) {
        desenharTituloCentral(g2, titulo(), 60, 8, w);

        int espaco = (tela == Tela.NOMES || tela == Tela.CLIENTE_CONECTAR || tela == Tela.MODO_JOGO) ? 14 : 22;
        int yInicial;

        if (tela == Tela.HOST_ESPERA) {
            desenharTextoCentral(g2, "SEU IP LOCAL:", 170, 3, w, TEXTO_BOTAO);
            desenharTituloCentral(g2, RedeBatalhaNaval.getIpLocal(), 210, 5, w);
            desenharTextoCentral(g2, statusRede, 275, 2, w, AVISO);
            yInicial = 330;
        } else if (tela == Tela.TRANSICAO) {
            desenharTituloCentral(g2, textoTransicao, 170, 6, w);
            desenharTextoCentral(g2, "O OUTRO JOGADOR NAO PODE OLHAR!", 250, 2, w, TEXTO_BOTAO);
            yInicial = 300;
        } else if (tela == Tela.FIM) {
            int v = jogo.vencedor();
            desenharTextoCentral(g2, jogo.nome(v) + " VENCEU!", 160, 5, w, TEXTO_BOTAO);
            for (int j = 0; j < 2; j++) {
                String linha = jogo.nome(j) + ": " + jogo.tiros(j) + " TIROS - " + jogo.acertos(j) + " ACERTOS";
                desenharTextoCentral(g2, linha, 220 + j * 28, 2, w, CINZA_METAL_1);
            }
            yInicial = 310;
        } else {
            int total = botoes.length * BOTAO_ALTURA + (botoes.length - 1) * espaco;
            yInicial = Math.max(150, (h - total) / 2 + 45);
        }

        int x = (w - BOTAO_LARGURA) / 2;
        for (int i = 0; i < botoes.length; i++) {
            Botao b = botoes[i];
            b.x = x;
            b.y = yInicial + i * (BOTAO_ALTURA + espaco);
            b.largura = BOTAO_LARGURA;
            b.altura = BOTAO_ALTURA;
            desenharBotao(g2, b, i == selecionado, true);
        }

        if (tela == Tela.CLIENTE_CONECTAR && !statusRede.isEmpty()) {
            Botao ultimo = botoes[botoes.length - 1];
            desenharTextoCentral(g2, statusRede, ultimo.y + ultimo.altura + 20, 2, w, AVISO);
        }

        if (tela == Tela.DIFICULDADE && selecionado >= 0 && selecionado <= 1) {
            String desc = selecionado == 0
                ? TAM_FACIL + "X" + TAM_FACIL + " - " + FROTA_FACIL.length + " NAVIOS"
                : TAM_DIFICIL + "X" + TAM_DIFICIL + " - " + FROTA_DIFICIL.length + " NAVIOS";
            Botao ultimo = botoes[botoes.length - 1];
            desenharTextoCentral(g2, desc, ultimo.y + ultimo.altura + 24, 2, w, TEXTO_BOTAO);
        }
    }

    private void desenharPosicionar(Graphics2D g2, int w, int h) {
        Tabuleiro t = jogo.tabuleiro(jogadorPosicionando);
        int n = t.tamanho();

        desenharTituloCentral(g2, "POSICIONE", 16, 5, w);
        desenharTextoCentral(g2, jogo.nome(jogadorPosicionando), 62, 3, w, CINZA_METAL_1);

        int proximo = t.proximoNaoColocado();
        String info;
        if (euPronto) {
            info = "FROTA CONFIRMADA!";
        } else if (proximo < 0) {
            info = "PRONTO! APERTE C OU CONFIRMAR";
        } else {
            info = "NAVIO " + (proximo + 1) + " DE " + t.quantidadeNavios()
              + " - TAMANHO " + t.tamanhoNavio(proximo)
              + " - " + (horizontal ? "HORIZONTAL" : "VERTICAL");
        }
        desenharTextoCentral(g2, info, 96, 2, w, TEXTO_BOTAO);

        int cel = Math.max(20, Math.min(48, (h - 270) / n));
        int lado = n * cel;
        int ox = (w - lado) / 2, oy = 140;

        desenharTabuleiro(g2, t, ox, oy, cel, true);

        if (!euPronto && proximo >= 0 && t.navioEm(cursorL, cursorC) < 0) {
            boolean cabe = t.cabe(proximo, cursorL, cursorC, horizontal);
            g2.setColor(cabe ? PREVIA_OK : PREVIA_RUIM);
            for (int k = 0; k < t.tamanhoNavio(proximo); k++) {
                int nl = horizontal ? cursorL : cursorL + k;
                int nc = horizontal ? cursorC + k : cursorC;
                if (t.dentro(nl, nc)) {
                    g2.fillRect(ox + nc * cel + 2, oy + nl * cel + 2, cel - 4, cel - 4);
                }
            }
        }
        if (!euPronto) {
            desenharCursor(g2, ox, oy, cel);
            registrarTabuleiro(ox, oy, cel, n);
        }

        String rodape = aviso.isEmpty() ? "R GIRA   A ALEATORIO   C CONFIRMA   ESC VOLTA" : aviso;
        desenharTextoCentral(g2, rodape, oy + lado + 14, 2, w, aviso.isEmpty() ? TEXTO_BOTAO : AVISO);

        desenharBotoesLinha(g2, w, oy + lado + 44, 200, 44, 20);
    }

    private void desenharBatalha(Graphics2D g2, int w, int h) {
        int eu = (modoRede || modoIA) ? meuIndice : jogo.vez();
        Tabuleiro meu = jogo.tabuleiro(eu);
        Tabuleiro alvo = jogo.tabuleiro(1 - eu);
        int n = alvo.tamanho();

        String tituloBatalha = (modoRede || modoIA)
            ? (jogo.vez() == meuIndice ? "SUA VEZ DE ATACAR" : "VEZ DE " + jogo.nome(1 - meuIndice))
            : "VEZ DE " + jogo.nome(eu);
        desenharTituloCentral(g2, tituloBatalha, 16, 4, w);

        int cel = Math.max(16, Math.min(44, Math.min((w - 140) / (2 * n), (h - 280) / n)));
        int lado = n * cel, gap = 80;
        int ox1 = (w - (2 * lado + gap)) / 2, ox2 = ox1 + lado + gap, oy = 120;

        desenharTextoCentralEm(g2, "SEU MAR", ox1 + lado / 2, oy - 46, 2, CINZA_METAL_1);
        desenharTextoCentralEm(g2, "ALVO: " + jogo.nome(1 - eu), ox2 + lado / 2, oy - 46, 2, CINZA_METAL_1);

        desenharTabuleiro(g2, meu, ox1, oy, cel, true);
        desenharTabuleiro(g2, alvo, ox2, oy, cel, false);

        boolean podeAtirar = (modoRede || modoIA)
            ? (jogo.vez() == meuIndice && !aguardandoRespostaTiro)
            : !aguardandoContinuar;
        if (podeAtirar) {
            desenharCursor(g2, ox2, oy, cel);
            registrarTabuleiro(ox2, oy, cel, n);
        }

        desenharTextoCentralEm(g2, "RESTAM " + meu.naviosRestantes(), ox1 + lado / 2, oy + lado + 12, 2, TEXTO_BOTAO);
        desenharTextoCentralEm(g2, "RESTAM " + alvo.naviosRestantes(), ox2 + lado / 2, oy + lado + 12, 2, TEXTO_BOTAO);

        String msg;
        Color cor;
        if (aguardandoContinuar && !modoRede && !modoIA) {
            msg = aviso;
            cor = ultimoResultado == Tabuleiro.ResultadoTiro.AGUA ? ERRO : AVISO;
        } else if (!aviso.isEmpty()) {
            msg = aviso;
            cor = AVISO;
        } else if ((modoRede || modoIA) && jogo.vez() != meuIndice) {
            msg = modoIA ? "IA PENSANDO..." : "AGUARDE O TIRO DO OPONENTE...";
            cor = CINZA_METAL_1;
        } else {
            msg = "ESCOLHA UM ALVO";
            cor = TEXTO_BOTAO;
        }
        int yMsg = oy + lado + 40;
        desenharTextoCentral(g2, msg, yMsg, 3, w, cor);

        if (aguardandoContinuar && !modoRede && !modoIA) {
            desenharBotoesLinha(g2, w, yMsg + 36, 240, 44, 20);
        }
    }

    private void registrarTabuleiro(int ox, int oy, int cel, int n) {
        tabAtivo = true;
        tabX = ox;
        tabY = oy;
        tabCel = cel;
        tabN = n;
    }

    private void desenharTabuleiro(Graphics2D g2, Tabuleiro t, int ox, int oy, int cel, boolean revelar) {
        int n = t.tamanho();
        int lado = n * cel;

        g2.setColor(CONTORNO);
        g2.fillRect(ox - 4, oy - 4, lado + 8, lado + 8);

        for (int c = 0; c < n; c++) {
            desenharPixelsTexto(g2, String.valueOf((char) ('A' + c)), ox + c * cel + (cel - 10) / 2, oy - 22, 2, CINZA_METAL_1);
        }
        for (int l = 0; l < n; l++) {
            String num = String.valueOf(l + 1);
            desenharPixelsTexto(g2, num, ox - 10 - larguraTexto(num, 2), oy + l * cel + (cel - 14) / 2, 2, CINZA_METAL_1);
        }

        // 1. Agua
        for (int l = 0; l < n; l++) {
            for (int c = 0; c < n; c++) {
                g2.setColor((l + c) % 2 == 0 ? AGUA : AGUA_CLARA);
                g2.fillRect(ox + c * cel, oy + l * cel, cel, cel);
            }
        }

        // 2. Navios (sprite inteiro; se faltar sprite, desenha celula por celula)
        for (int id = 0; id < t.quantidadeNavios(); id++) {
            if (!t.estaColocado(id)) continue;
            boolean afundado = t.afundado(id);
            if (!revelar && !afundado) continue;
            if (desenharNavioComSprite(g2, t, id, cel, ox, oy, afundado)) continue;
            for (int l = 0; l < n; l++) {
                for (int c = 0; c < n; c++) {
                    if (t.navioEm(l, c) == id) {
                        desenharParteNavio(g2, t, l, c, ox + c * cel, oy + l * cel, cel);
                    }
                }
            }
        }

        // 3. Marcas de tiro (por cima dos navios)
        for (int l = 0; l < n; l++) {
            for (int c = 0; c < n; c++) {
                if (!t.foiAtingida(l, c)) continue;
                int x = ox + c * cel;
                int y = oy + l * cel;
                if (t.navioEm(l, c) >= 0) desenharAcerto(g2, x, y, cel);
                else desenharErro(g2, x, y, cel);
            }
        }
    }

    /** Desenha o navio com o sprite do seu tamanho. Retorna false se nao houver sprite. */
    private boolean desenharNavioComSprite(Graphics2D g2, Tabuleiro t, int id, int cel, int ox, int oy, boolean afundado) {
        int n = t.tamanho();
        int minL = n, minC = n, maxL = -1, maxC = -1;
        for (int l = 0; l < n; l++) {
            for (int c = 0; c < n; c++) {
                if (t.navioEm(l, c) == id) {
                    minL = Math.min(minL, l); maxL = Math.max(maxL, l);
                    minC = Math.min(minC, c); maxC = Math.max(maxC, c);
                }
            }
        }
        if (maxL < 0) return false;

        boolean vertical = maxL > minL && maxC == minC;
        int tam = t.tamanhoNavio(id);
        BufferedImage sprite = (vertical ? SPRITES_VERTICAIS : SPRITES).get(tam);
        if (sprite == null) return false;

        if (afundado) {
            sprite = new RescaleOp(new float[]{0.5f, 0.5f, 0.5f, 1f}, new float[]{0f, 0f, 0f, 0f}, null).filter(sprite, null);
        }

        // Comprimento do navio em pixels (um pouco menor que as celulas, para respirar)
        double comprimento = tam * cel - 4;
        double referencia = vertical ? sprite.getHeight() : sprite.getWidth();
        double escala = comprimento / referencia;
        int dw = (int) Math.round(sprite.getWidth() * escala);
        int dh = (int) Math.round(sprite.getHeight() * escala);

        // Centro da area ocupada pelo navio
        double cx = ox + (minC + maxC + 1) * cel / 2.0;
        double cy = oy + (minL + maxL + 1) * cel / 2.0;

        Object interpolacao = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(sprite, (int) Math.round(cx - dw / 2.0), (int) Math.round(cy - dh / 2.0), dw, dh, null);
        if (interpolacao != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacao);
        }
        return true;
    }

    private void desenharCursor(Graphics2D g2, int ox, int oy, int cel) {
        int x = ox + cursorC * cel, y = oy + cursorL * cel;
        g2.setColor(CINZA_METAL_1);
        g2.drawRect(x + 1, y + 1, cel - 3, cel - 3);
        g2.drawRect(x + 2, y + 2, cel - 5, cel - 5);
    }

    private void desenharParteNavio(Graphics2D g2, Tabuleiro t, int l, int c, int x, int y, int cel) {
        int id = t.navioEm(l, c);
        int m = Math.max(3, cel / 8);
        boolean esq = t.navioEm(l, c - 1) == id, dir = t.navioEm(l, c + 1) == id;
        boolean cima = t.navioEm(l - 1, c) == id, baixo = t.navioEm(l + 1, c) == id;

        int x0 = x + (esq ? 0 : m), y0 = y + (cima ? 0 : m);
        int x1 = x + cel - (dir ? 0 : m), y1 = y + cel - (baixo ? 0 : m);
        boolean afundou = t.afundado(id);

        g2.setColor(afundou ? AFUNDADO_ESC : CASCO_ESCURO);
        g2.fillRect(x0, y0, x1 - x0, y1 - y0);
        g2.setColor(afundou ? AFUNDADO : CASCO);
        g2.fillRect(x0, y0, x1 - x0, (y1 - y0) - (baixo ? 0 : Math.max(2, m / 2 + 1)));
    }

    private void desenharAcerto(Graphics2D g2, int x, int y, int cel) {
        int s = Math.max(6, cel / 2);
        int ax = x + (cel - s) / 2, ay = y + (cel - s) / 2;
        g2.setColor(new Color(90, 10, 10));
        g2.fillRect(ax - 2, ay - 2, s + 4, s + 4);
        g2.setColor(ACERTO);
        g2.fillRect(ax, ay, s, s);
        int s2 = Math.max(2, s / 3);
        g2.setColor(new Color(255, 170, 60));
        g2.fillRect(ax + (s - s2) / 2, ay + (s - s2) / 2, s2, s2);
    }

    private void desenharErro(Graphics2D g2, int x, int y, int cel) {
        int s = Math.max(4, cel / 5);
        int cx = x + cel / 2, cy = y + cel / 2;
        g2.setColor(new Color(230, 230, 230, 90));
        g2.drawRect(cx - s, cy - s, s * 2 - 1, s * 2 - 1);
        g2.setColor(ERRO);
        g2.fillRect(cx - s / 2, cy - s / 2, s, s);
    }

    private String titulo() {
        switch (tela) {
            case OPCOES:           return "OPCOES";
            case MODO_JOGO:        return "MODO DE JOGO";
            case DIFICULDADE:      return "DIFICULDADE";
            case NOMES:            return (modoRede || modoIA) ? "SEU NOME" : "JOGADORES";
            case HOST_ESPERA:      return "SALA CRIADA";
            case CLIENTE_CONECTAR: return "ENTRAR NA SALA";
            case TRANSICAO:        return "PASSE PARA";
            case FIM:              return "FIM DE JOGO";
            default:               return "BATALHA NAVAL";
        }
    }

    private void desenharOndas(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(255, 255, 255, 25));
        for (int y = 140; y < h; y += 40) {
            for (int x = 0; x < w; x += 48) {
                g2.fillRect(x + ((y / 40) % 2) * 24, y, 24, 4);
            }
        }
    }

    // ===== Texto =====
    private int escalaQueCabe(String texto, int escalaMax, int larguraMax) {
        int e = escalaMax;
        while (e > 1 && larguraTexto(texto, e) > larguraMax) e--;
        return e;
    }

    private void desenharTituloCentral(Graphics2D g2, String texto, int y, int escalaMax, int w) {
        int e = escalaQueCabe(texto, escalaMax, w - 40);
        desenharTextoMetalico(g2, texto, (w - larguraTexto(texto, e)) / 2, y, e);
    }

    private void desenharTextoCentral(Graphics2D g2, String texto, int y, int escalaMax, int w, Color cor) {
        desenharTextoCentralEm(g2, texto, w / 2, y, escalaQueCabe(texto, escalaMax, w - 40), cor);
    }

    private void desenharTextoCentralEm(Graphics2D g2, String texto, int cx, int y, int escala, Color cor) {
        int x = cx - larguraTexto(texto, escala) / 2;
        desenharPixelsTexto(g2, texto, x + Math.max(1, escala / 2), y + Math.max(1, escala / 2), escala, new Color(0, 0, 0, 140));
        desenharPixelsTexto(g2, texto, x, y, escala, cor);
    }

    private void desenharTextoMetalico(Graphics2D g2, String texto, int x, int y, int escala) {
        desenharPixelsTexto(g2, texto, x + escala, y + escala, escala, new Color(0, 0, 0, 160));

        int altura = 7 * escala;
        int largura = larguraTexto(texto, escala) + 1;

        BufferedImage mascara = new BufferedImage(largura, altura + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gm = mascara.createGraphics();
        desenharPixelsTexto(gm, texto, 0, 0, escala, Color.WHITE);
        gm.dispose();

        BufferedImage corpo = new BufferedImage(largura, altura + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = corpo.createGraphics();
        gc.setPaint(new GradientPaint(0, 0, CINZA_METAL_1, 0, altura, CINZA_METAL_3));
        gc.fillRect(0, 0, corpo.getWidth(), corpo.getHeight());
        gc.setComposite(AlphaComposite.DstIn);
        gc.drawImage(mascara, 0, 0, null);
        gc.dispose();

        g2.drawImage(corpo, x, y, null);
        desenharPixelsTexto(g2, texto, x, y, escala, new Color(30, 34, 40, 90));
    }

    // ===== Botoes =====
    private void desenharBotoesLinha(Graphics2D g2, int w, int y, int largura, int altura, int gap) {
        int total = botoes.length * largura + (botoes.length - 1) * gap;
        int bx = (w - total) / 2;
        for (int i = 0; i < botoes.length; i++) {
            Botao b = botoes[i];
            b.x = bx + i * (largura + gap);
            b.y = y;
            b.largura = largura;
            b.altura = altura;
            desenharBotao(g2, b, i == selecionado, false);
        }
    }

    private void retanguloChanfrado(Graphics2D g2, int x, int y, int w, int h, int p) {
        g2.fillRect(x + p, y, w - 2 * p, h);
        g2.fillRect(x, y + p, w, h - 2 * p);
    }

    private void desenharBotao(Graphics2D g2, Botao b, boolean ativo, boolean comSeta) {
        int x = b.x, y = b.y, w = b.largura, h = b.altura;
        int borda = 6;

        g2.setColor(VERDE_SOMBRA);
        retanguloChanfrado(g2, x + 4, y + 4, w, h, PIXEL);

        g2.setColor(CONTORNO);
        retanguloChanfrado(g2, x, y, w, h, PIXEL);

        g2.setColor(ativo ? VERDE_CLARO : VERDE_BASE);
        g2.fillRect(x + borda, y + borda, w - borda * 2, h - borda * 2);

        g2.setColor(new Color(255, 255, 255, ativo ? 50 : 25));
        g2.fillRect(x + borda, y + borda, w - borda * 2, PIXEL);

        if (ativo && comSeta) {
            g2.setColor(CINZA_METAL_1);
            g2.fillRect(x - 18, y + h / 2 - 6, 12, 12);
        }

        String texto = b.campo ? textoCampo(b.indice, ativo) : b.rotulo;

        int escala = escalaQueCabe(texto, h >= 50 ? 4 : 3, w - 40);
        int lt = larguraTexto(texto, escala);
        int lh = 7 * escala;
        int tx = x + (w - lt) / 2;
        int ty = y + (h - lh) / 2;
        desenharPixelsTexto(g2, texto, tx + 2, ty + 2, escala, new Color(0, 0, 0, 140));
        desenharPixelsTexto(g2, texto, tx, ty, escala, TEXTO_BOTAO);
    }

    private String textoCampo(int i, boolean ativo) {
        if (tela == Tela.CLIENTE_CONECTAR) {
            if (i == 0) {
                String n = nomes[1];
                if (!ativo && n.isEmpty()) return "SEU NOME";
                return ativo ? n + "_" : n;
            } else {
                if (!ativo && ipDigitado.isEmpty()) return "IP DO HOST";
                return ativo ? ipDigitado + "_" : ipDigitado;
            }
        }
        int idx = modoRede ? meuIndice : i;
        String n = nomes[idx];
        if (!ativo && n.isEmpty()) return (modoRede || modoIA) ? "SEU NOME" : "JOGADOR " + (i + 1);
        return ativo ? n + "_" : n;
    }

    // ===== Fonte bitmap 5x7 (pixel art) =====
    private void desenharPixelsTexto(Graphics2D g2, String texto, int x, int y, int escala, Color cor) {
        g2.setColor(cor);
        int cursor = x;
        for (char c : texto.toUpperCase().toCharArray()) {
            boolean[][] glifo = Fonte.glifo(c);
            for (int linha = 0; linha < 7; linha++) {
                for (int col = 0; col < 5; col++) {
                    if (glifo[linha][col]) {
                        g2.fillRect(cursor + col * escala, y + linha * escala, escala, escala);
                    }
                }
            }
            cursor += 6 * escala;
        }
    }

    private int larguraTexto(String texto, int escala) {
        return Math.max(1, texto.length() * 6 * escala - escala);
    }

    // ===== Classes auxiliares =====
    private static class Botao {
        final int indice;
        final boolean campo;
        String rotulo;
        int x, y, largura, altura;

        Botao(String rotulo, int indice) {
            this(rotulo, indice, false);
        }

        Botao(String rotulo, int indice, boolean campo) {
            this.rotulo = rotulo;
            this.indice = indice;
            this.campo = campo;
        }

        boolean contem(int px, int py) {
            return px >= x && px <= x + largura && py >= y && py <= y + altura;
        }
    }

    static class Fonte {
        private static final java.util.Map<Character, String[]> GLIFOS = new java.util.HashMap<>();
        static {
            GLIFOS.put('A', new String[]{" ### ","#   #","#   #","#####","#   #","#   #","#   #"});
            GLIFOS.put('B', new String[]{"#### ","#   #","#   #","#### ","#   #","#   #","#### "});
            GLIFOS.put('C', new String[]{" ### ","#   #","#    ","#    ","#    ","#   #"," ### "});
            GLIFOS.put('D', new String[]{"#### ","#   #","#   #","#   #","#   #","#   #","#### "});
            GLIFOS.put('E', new String[]{"#####","#    ","#    ","#### ","#    ","#    ","#####"});
            GLIFOS.put('F', new String[]{"#####","#    ","#    ","#### ","#    ","#    ","#    "});
            GLIFOS.put('G', new String[]{" ### ","#   #","#    ","# ###","#   #","#   #"," ####"});
            GLIFOS.put('H', new String[]{"#   #","#   #","#   #","#####","#   #","#   #","#   #"});
            GLIFOS.put('I', new String[]{" ### ","  #  ","  #  ","  #  ","  #  ","  #  "," ### "});
            GLIFOS.put('J', new String[]{"  ###","   # ","   # ","   # ","   # ","#  # "," ##  "});
            GLIFOS.put('K', new String[]{"#   #","#  # ","# #  ","##   ","# #  ","#  # ","#   #"});
            GLIFOS.put('L', new String[]{"#    ","#    ","#    ","#    ","#    ","#    ","#####"});
            GLIFOS.put('M', new String[]{"#   #","## ##","# # #","# # #","#   #","#   #","#   #"});
            GLIFOS.put('N', new String[]{"#   #","##  #","# # #","#  ##","#   #","#   #","#   #"});
            GLIFOS.put('O', new String[]{" ### ","#   #","#   #","#   #","#   #","#   #"," ### "});
            GLIFOS.put('P', new String[]{"#### ","#   #","#   #","#### ","#    ","#    ","#    "});
            GLIFOS.put('Q', new String[]{" ### ","#   #","#   #","#   #","# # #","#  # "," ## #"});
            GLIFOS.put('R', new String[]{"#### ","#   #","#   #","#### ","# #  ","#  # ","#   #"});
            GLIFOS.put('S', new String[]{" ####","#    ","#    "," ### ","    #","    #","#### "});
            GLIFOS.put('T', new String[]{"#####","  #  ","  #  ","  #  ","  #  ","  #  ","  #  "});
            GLIFOS.put('U', new String[]{"#   #","#   #","#   #","#   #","#   #","#   #"," ### "});
            GLIFOS.put('V', new String[]{"#   #","#   #","#   #","#   #","#   #"," # # ","  #  "});
            GLIFOS.put('W', new String[]{"#   #","#   #","#   #","# # #","# # #","## ##","#   #"});
            GLIFOS.put('X', new String[]{"#   #","#   #"," # # ","  #  "," # # ","#   #","#   #"});
            GLIFOS.put('Y', new String[]{"#   #","#   #"," # # ","  #  ","  #  ","  #  ","  #  "});
            GLIFOS.put('Z', new String[]{"#####","    #","   # ","  #  "," #   ","#    ","#####"});
            GLIFOS.put('0', new String[]{" ### ","#   #","#  ##","# # #","##  #","#   #"," ### "});
            GLIFOS.put('1', new String[]{"  #  "," ##  ","  #  ","  #  ","  #  ","  #  "," ### "});
            GLIFOS.put('2', new String[]{" ### ","#   #","    #","   # ","  #  "," #   ","#####"});
            GLIFOS.put('3', new String[]{"#### ","    #","    #"," ### ","    #","    #","#### "});
            GLIFOS.put('4', new String[]{"   # ","  ## "," # # ","#  # ","#####","   # ","   # "});
            GLIFOS.put('5', new String[]{"#####","#    ","#### ","    #","    #","#   #"," ### "});
            GLIFOS.put('6', new String[]{" ### ","#    ","#    ","#### ","#   #","#   #"," ### "});
            GLIFOS.put('7', new String[]{"#####","    #","   # ","  #  "," #   "," #   "," #   "});
            GLIFOS.put('8', new String[]{" ### ","#   #","#   #"," ### ","#   #","#   #"," ### "});
            GLIFOS.put('9', new String[]{" ### ","#   #","#   #"," ####","    #","    #"," ### "});
            GLIFOS.put(':', new String[]{"     ","  #  ","  #  ","     ","  #  ","  #  ","     "});
            GLIFOS.put('!', new String[]{"  #  ","  #  ","  #  ","  #  ","  #  ","     ","  #  "});
            GLIFOS.put('?', new String[]{" ### ","#   #","    #","   # ","  #  ","     ","  #  "});
            GLIFOS.put('-', new String[]{"     ","     ","     "," ### ","     ","     ","     "});
            GLIFOS.put('.', new String[]{"     ","     ","     ","     ","     ","     ","  #  "});
            GLIFOS.put('(', new String[]{"   # ","  #  "," #   "," #   "," #   ","  #  ","   # "});
            GLIFOS.put(')', new String[]{" #   ","  #  ","   # ","   # ","   # ","  #  "," #   "});
            GLIFOS.put('/', new String[]{"    #","    #","   # ","  #  "," #   ","#    ","#    "});
            GLIFOS.put('_', new String[]{"     ","     ","     ","     ","     ","     ","#####"});
            GLIFOS.put(' ', new String[]{"     ","     ","     ","     ","     ","     ","     "});
        }

        static boolean[][] glifo(char c) {
            String[] linhas = GLIFOS.getOrDefault(c, GLIFOS.get(' '));
            boolean[][] m = new boolean[7][5];
            for (int l = 0; l < 7; l++) {
                for (int col = 0; col < 5; col++) {
                    m[l][col] = linhas[l].charAt(col) == '#';
                }
            }
            return m;
        }
    }

    // ===== Main =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Batalha Naval");
            MenuBatalhaNaval menu = new MenuBatalhaNaval(janela);
            janela.add(menu);
            janela.pack();
            janela.setMinimumSize(janela.getSize());
            janela.setResizable(true);
            janela.setLocationRelativeTo(null);
            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setVisible(true);
            menu.requestFocusInWindow();
        });
    }
}