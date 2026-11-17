import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
import javax.swing.*;

public class MenuBatalhaNaval extends JPanel {

    // ===== Paleta de cores =====
    private static final Color AZUL_TOPO     = new Color(10, 40, 90);
    private static final Color AZUL_FUNDO    = new Color(30, 110, 170);
    private static final Color CINZA_METAL_1 = new Color(200, 205, 212);
    private static final Color CINZA_METAL_2 = new Color(120, 128, 138);
    private static final Color CINZA_METAL_3 = new Color(60, 66, 74);
    private static final Color VERDE_BASE    = new Color(51, 82, 36);
    private static final Color VERDE_CLARO   = new Color(78, 110, 56);
    private static final Color VERDE_SOMBRA  = new Color(28, 45, 20);
    private static final Color TEXTO_BOTAO   = new Color(215, 220, 190);

    private static final Color AGUA         = new Color(20, 80, 130);
    private static final Color AGUA_CLARA   = new Color(40, 110, 165);
    private static final Color CASCO        = new Color(150, 156, 164);
    private static final Color CASCO_ESCURO = new Color(90, 96, 104);
    private static final Color ACERTO       = new Color(200, 40, 40);
    private static final Color ERRO         = new Color(230, 230, 230);

    private static final int PIXEL = 6;
    private static final int BOTAO_LARGURA = 380;
    private static final int BOTAO_ALTURA = 56;
    private static final int MAX_NOME = 12;

    // ===== Tabuleiro =====
    private static final int TAM_TABULEIRO = 6;
    private static final int TAM_CELULA = 48;
    private static final int[] NAVIOS = {3, 2, 1, 1};

    private enum Tela { MENU, OPCOES, DIFICULDADE, NOMES, POSICIONAR }

    private final JFrame janela;
    private Tela tela = Tela.MENU;
    private boolean telaCheia = false;
    private Botao[] botoes;
    private int selecionado = 0;

    private String dificuldade = "FACIL";
    private final String[] nomes = {"", ""};

    // Posicionamento: cada célula guarda o índice do navio (-1 = vazio)
    private int[][] tabuleiro = new int[TAM_TABULEIRO][TAM_TABULEIRO];
    private boolean[] navioColocado = new boolean[NAVIOS.length];
    private boolean horizontal = true;
    private int cursorL = 0, cursorC = 0;
    private int navioAtual = 0;

    public MenuBatalhaNaval(JFrame janela) {
        this.janela = janela;
        setPreferredSize(new Dimension(640, 480));
        setFocusable(true);
        montarMenu();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (tela == Tela.POSICIONAR) {
                    int[] cel = celulaEm(e.getX(), e.getY());
                    if (cel != null) {
                        cursorL = cel[0];
                        cursorC = cel[1];
                        repaint();
                    }
                    return;
                }
                for (int i = 0; i < botoes.length; i++) {
                    if (botoes[i].contem(e.getX(), e.getY())) {
                        selecionado = i;
                        repaint();
                    }
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (tela == Tela.POSICIONAR) {
                    int[] cel = celulaEm(e.getX(), e.getY());
                    if (cel != null) {
                        cliqueNaCelula(cel[0], cel[1]);
                        repaint();
                    } else {
                        for (int i = 0; i < botoes.length; i++) {
                            if (botoes[i].contem(e.getX(), e.getY())) {
                                acionar(botoes[i].indice);
                                break;
                            }
                        }
                    }
                    return;
                }
                for (int i = 0; i < botoes.length; i++) {
                    if (botoes[i].contem(e.getX(), e.getY())) {
                        if (tela == Tela.NOMES && botoes[i].campo) {
                            selecionado = i;
                            repaint();
                        } else {
                            acionar(botoes[i].indice);
                        }
                        break;
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (tela == Tela.POSICIONAR) {
                    teclaPosicionar(e);
                    repaint();
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        selecionado = (selecionado - 1 + botoes.length) % botoes.length;
                        break;
                    case KeyEvent.VK_DOWN:
                        selecionado = (selecionado + 1) % botoes.length;
                        break;
                    case KeyEvent.VK_ENTER:
                        acionar(botoes[selecionado].indice);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        voltarTela();
                        break;
                }
                repaint();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (tela != Tela.NOMES || selecionado > 1) return;
                int i = selecionado;
                char c = e.getKeyChar();

                if (c == '\b') {
                    if (!nomes[i].isEmpty()) {
                        nomes[i] = nomes[i].substring(0, nomes[i].length() - 1);
                    }
                } else {
                    String limpo = normalizar(String.valueOf(c)).toUpperCase();
                    for (char ch : limpo.toCharArray()) {
                        boolean valido = (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == ' ';
                        if (valido && nomes[i].length() < MAX_NOME) {
                            nomes[i] += ch;
                        }
                    }
                }
                repaint();
            }
        });
    }

    // ===== Troca de telas =====
    private void montarMenu() {
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

    private void montarDificuldade() {
        tela = Tela.DIFICULDADE;
        selecionado = 0;
        botoes = new Botao[] {
            new Botao("FACIL",  0),
            new Botao("DIFICIL", 1),
            new Botao("VOLTAR", 2)
        };
    }

    private void montarNomes() {
        tela = Tela.NOMES;
        selecionado = 0;
        botoes = new Botao[] {
            new Botao("", 0, true),
            new Botao("", 1, true),
            new Botao("CONFIRMAR", 2),
            new Botao("VOLTAR", 3)
        };
    }

    private void montarPosicionar() {
        tela = Tela.POSICIONAR;
        tabuleiro = new int[TAM_TABULEIRO][TAM_TABULEIRO];
        for (int[] linha : tabuleiro) java.util.Arrays.fill(linha, -1);
        navioColocado = new boolean[NAVIOS.length];
        navioAtual = 0;
        horizontal = true;
        cursorL = 0;
        cursorC = 0;
        botoes = new Botao[] {
            new Botao("CONFIRMAR", 0),
            new Botao("VOLTAR", 1)
        };
        selecionado = 0;
    }

    private void voltarTela() {
        switch (tela) {
            case OPCOES:      montarMenu(); break;
            case DIFICULDADE: montarMenu(); break;
            case NOMES:       montarDificuldade(); break;
            case POSICIONAR:  montarNomes(); break;
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
                    case 0: montarDificuldade(); break;
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

            case DIFICULDADE:
                switch (indice) {
                    case 0: dificuldade = "FACIL";  montarNomes(); break;
                    case 1: dificuldade = "DIFICIL"; montarNomes(); break;
                    case 2: montarMenu(); break;
                }
                break;

            case NOMES:
                switch (indice) {
                    case 0: selecionado = 1; break;
                    case 1: selecionado = 2; break;
                    case 2: confirmarNomes(); break;
                    case 3: montarDificuldade(); break;
                }
                break;

            case POSICIONAR:
                switch (indice) {
                    case 0: confirmarPosicionamento(); break;
                    case 1: montarNomes(); break;
                }
                break;
        }
        repaint();
    }

    private void confirmarNomes() {
        if (dificuldade.equals("FACIL")) {
            montarPosicionar();
        } else {
            String j1 = nomes[0].isEmpty() ? "JOGADOR 1" : nomes[0];
            String j2 = nomes[1].isEmpty() ? "JOGADOR 2" : nomes[1];
            System.out.println("Iniciar jogo: " + j1 + " vs " + j2 + " (" + dificuldade + ")");
        }
    }

    private void confirmarPosicionamento() {
        if (!todosColocados()) return;
        String j1 = nomes[0].isEmpty() ? "JOGADOR 1" : nomes[0];
        String j2 = nomes[1].isEmpty() ? "JOGADOR 2" : nomes[1];
        System.out.println("Iniciar jogo: " + j1 + " vs " + j2 + " (" + dificuldade + ")");
        // Aqui entra a abertura da tela de jogo, usando 'tabuleiro'.
    }

    private static String normalizar(String s) {
    // Remove acentos: "João" vira "Joao"
    return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
}

    private boolean todosColocados() {
        for (boolean b : navioColocado) if (!b) return false;
        return true;
    }

    // ===== Lógica de posicionamento =====
    private void teclaPosicionar(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    cursorL = Math.max(0, cursorL - 1); break;
            case KeyEvent.VK_DOWN:  cursorL = Math.min(TAM_TABULEIRO - 1, cursorL + 1); break;
            case KeyEvent.VK_LEFT:  cursorC = Math.max(0, cursorC - 1); break;
            case KeyEvent.VK_RIGHT: cursorC = Math.min(TAM_TABULEIRO - 1, cursorC + 1); break;
            case KeyEvent.VK_R:     horizontal = !horizontal; break;
            case KeyEvent.VK_ENTER: cliqueNaCelula(cursorL, cursorC); break;
            case KeyEvent.VK_ESCAPE: montarNomes(); break;
            case KeyEvent.VK_BACK_SPACE: remover(cursorL, cursorC); break;
        }
    }

    private void cliqueNaCelula(int l, int c) {
        // Se a célula já tem navio, remove aquele navio
        if (tabuleiro[l][c] >= 0) {
            remover(l, c);
            return;
        }
        // Coloca o próximo navio ainda não colocado
        if (navioAtual >= NAVIOS.length) return;
        int tam = NAVIOS[navioAtual];
        if (cabeCom(l, c, tam)) {
            for (int k = 0; k < tam; k++) {
                int dl = horizontal ? 0 : k;
                int dc = horizontal ? k : 0;
                tabuleiro[l + dl][c + dc] = navioAtual;
            }
            navioColocado[navioAtual] = true;
            navioAtual = proximoNaoColocado();
        }
    }

    private void remover(int l, int c) {
        int id = tabuleiro[l][c];
        if (id < 0) return;
        for (int i = 0; i < TAM_TABULEIRO; i++) {
            for (int j = 0; j < TAM_TABULEIRO; j++) {
                if (tabuleiro[i][j] == id) tabuleiro[i][j] = -1;
            }
        }
        navioColocado[id] = false;
        if (navioAtual >= NAVIOS.length || id < navioAtual) navioAtual = id;
    }

    private boolean cabeCom(int l, int c, int tam) {
        for (int k = 0; k < tam; k++) {
            int dl = horizontal ? 0 : k;
            int dc = horizontal ? k : 0;
            int nl = l + dl, nc = c + dc;
            if (nl >= TAM_TABULEIRO || nc >= TAM_TABULEIRO) return false;
            if (tabuleiro[nl][nc] >= 0) return false;
        }
        return true;
    }

    private int proximoNaoColocado() {
        for (int i = 0; i < NAVIOS.length; i++) {
            if (!navioColocado[i]) return i;
        }
        return NAVIOS.length;
    }

    // Retorna {linha, coluna} da célula sob o ponteiro, ou null
    private int[] celulaEm(int px, int py) {
        int ox = origemTabuleiroX(), oy = origemTabuleiroY();
        int c = (px - ox) / TAM_CELULA;
        int l = (py - oy) / TAM_CELULA;
        if (px < ox || py < oy || c >= TAM_TABULEIRO || l >= TAM_TABULEIRO) return null;
        return new int[]{l, c};
    }

    private int origemTabuleiroX() {
        return (getWidth() - TAM_TABULEIRO * TAM_CELULA) / 2;
    }

    private int origemTabuleiroY() {
        return 150;
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

        if (tela == Tela.POSICIONAR) {
            desenharPosicionar(g2, w, h);
            g2.dispose();
            return;
        }

        String titulo = titulo();
        int escalaTitulo = 8;
        int xTitulo = (w - larguraTexto(titulo, escalaTitulo)) / 2;
        desenharTextoMetalico(g2, titulo, xTitulo, 80, escalaTitulo);

        int yInicial = (tela == Tela.NOMES) ? 180 : 220;
        int espaco   = (tela == Tela.NOMES) ? 16 : 22;
        int x = (w - BOTAO_LARGURA) / 2;

        for (int i = 0; i < botoes.length; i++) {
            Botao b = botoes[i];
            b.x = x;
            b.y = yInicial + i * (BOTAO_ALTURA + espaco);
            b.largura = BOTAO_LARGURA;
            b.altura = BOTAO_ALTURA;
            desenharBotao(g2, b, i == selecionado);
        }

        g2.dispose();
    }

    private void desenharPosicionar(Graphics2D g2, int w, int h) {
        String titulo = "POSICIONE";
        int escalaTitulo = 5;
        int xTitulo = (w - larguraTexto(titulo, escalaTitulo)) / 2;
        desenharTextoMetalico(g2, titulo, xTitulo, 30, escalaTitulo);

        // Mensagem com o navio atual
        String info = todosColocados()
            ? "PRONTO"
            : "NAVIO " + (navioAtual + 1) + " TAM " + NAVIOS[navioAtual]
              + (horizontal ? " H" : " V");
        int escalaInfo = 2;
        desenharPixelsTexto(g2, info, (w - larguraTexto(info, escalaInfo)) / 2, 90, escalaInfo, TEXTO_BOTAO);

        // Tabuleiro
        int ox = origemTabuleiroX(), oy = origemTabuleiroY();
        int tam = TAM_TABULEIRO * TAM_CELULA;

        g2.setColor(new Color(15, 25, 10));
        g2.fillRect(ox - 4, oy - 4, tam + 8, tam + 8);

        for (int l = 0; l < TAM_TABULEIRO; l++) {
            for (int c = 0; c < TAM_TABULEIRO; c++) {
                int x = ox + c * TAM_CELULA;
                int y = oy + l * TAM_CELULA;
                g2.setColor((l + c) % 2 == 0 ? AGUA : AGUA_CLARA);
                g2.fillRect(x, y, TAM_CELULA, TAM_CELULA);

                if (tabuleiro[l][c] >= 0) {
                    desenharParteNavio(g2, x, y);
                }
                if (l == cursorL && c == cursorC) {
                    g2.setColor(CINZA_METAL_1);
                    g2.drawRect(x + 1, y + 1, TAM_CELULA - 3, TAM_CELULA - 3);
                    g2.drawRect(x + 2, y + 2, TAM_CELULA - 5, TAM_CELULA - 5);
                }
            }
        }

        // Botões na parte inferior
        int yBotoes = oy + tam + 24;
        int larguraBotao = 240, alturaBotao = 44;
        int bx = (w - (larguraBotao * 2 + 20)) / 2;
        for (int i = 0; i < botoes.length; i++) {
            Botao b = botoes[i];
            b.x = bx + i * (larguraBotao + 20);
            b.y = yBotoes;
            b.largura = larguraBotao;
            b.altura = alturaBotao;
            desenharBotao(g2, b, i == selecionado);
        }
    }

    private void desenharParteNavio(Graphics2D g2, int x, int y) {
        int m = 6;
        g2.setColor(CASCO_ESCURO);
        g2.fillRect(x + m, y + m, TAM_CELULA - m * 2, TAM_CELULA - m * 2);
        g2.setColor(CASCO);
        g2.fillRect(x + m, y + m, TAM_CELULA - m * 2 - PIXEL, TAM_CELULA - m * 2 - PIXEL);
    }

    private String titulo() {
        switch (tela) {
            case OPCOES:      return "OPCOES";
            case DIFICULDADE: return "DIFICULDADE";
            case NOMES:       return "JOGADORES";
            default:          return "BATALHA NAVAL";
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

    private void desenharBotao(Graphics2D g2, Botao b, boolean ativo) {
        int x = b.x, y = b.y, w = b.largura, h = b.altura;
        int borda = 6;

        g2.setColor(VERDE_SOMBRA);
        g2.fillRect(x + 4, y + 4, w, h);

        g2.setColor(new Color(15, 25, 10));
        g2.fillRect(x, y, w, h);

        g2.setColor(ativo ? VERDE_CLARO : VERDE_BASE);
        g2.fillRect(x + borda, y + borda, w - borda * 2, h - borda * 2);

        g2.setColor(new Color(255, 255, 255, ativo ? 50 : 25));
        g2.fillRect(x + borda, y + borda, w - borda * 2, PIXEL);

        g2.setColor(getBackground());
        g2.fillRect(x, y, PIXEL, PIXEL);
        g2.fillRect(x + w - PIXEL, y, PIXEL, PIXEL);
        g2.fillRect(x, y + h - PIXEL, PIXEL, PIXEL);
        g2.fillRect(x + w - PIXEL, y + h - PIXEL, PIXEL, PIXEL);

        if (ativo && tela != Tela.POSICIONAR) {
            g2.setColor(CINZA_METAL_1);
            g2.fillRect(x - 18, y + h / 2 - 6, 12, 12);
        }

        String texto = b.campo ? textoCampo(b.indice, ativo) : b.rotulo;

        int escala = (tela == Tela.POSICIONAR) ? 3 : 4;
        int lt = larguraTexto(texto, escala);
        int lh = 7 * escala;
        int tx = x + (w - lt) / 2;
        int ty = y + (h - lh) / 2;
        desenharPixelsTexto(g2, texto, tx + 2, ty + 2, escala, new Color(0, 0, 0, 140));
        desenharPixelsTexto(g2, texto, tx, ty, escala, TEXTO_BOTAO);
    }

    private String textoCampo(int i, boolean ativo) {
        String n = nomes[i];
        if (!ativo && n.isEmpty()) return "JOGADOR " + (i + 1);
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
            GLIFOS.put(':', new String[]{"     ","  ##  ","  ##  ","     ","  ##  ","  ##  ","     "});
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
            janela.setResizable(true);
            janela.setLocationRelativeTo(null);
            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setVisible(true);
            menu.requestFocusInWindow();
        });
    }
}