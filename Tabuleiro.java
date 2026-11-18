import java.util.Arrays;
import java.util.Random;

/**
 * Tabuleiro de um jogador: guarda onde estao os navios e quais celulas
 * ja receberam tiro. Nao sabe nada de interface.
 */
public class Tabuleiro {

    public enum ResultadoTiro { AGUA, ACERTO, AFUNDOU, REPETIDO, INVALIDO }

    private final int tamanho;
    private final int[] frota;          // tamanho de cada navio
    private final int[][] navio;        // indice do navio na celula (-1 = agua)
    private final boolean[][] atingida; // celula ja recebeu tiro
    private final boolean[] colocado;
    private final int[] acertos;        // acertos recebidos por navio

    public Tabuleiro(int tamanho, int[] frota) {
        this.tamanho = tamanho;
        this.frota = frota.clone();
        this.navio = new int[tamanho][tamanho];
        this.atingida = new boolean[tamanho][tamanho];
        this.colocado = new boolean[frota.length];
        this.acertos = new int[frota.length];
        limpar();
    }

    public int tamanho()             { return tamanho; }
    public int quantidadeNavios()    { return frota.length; }
    public int tamanhoNavio(int id)  { return frota[id]; }

    public void limpar() {
        for (int[] linha : navio) Arrays.fill(linha, -1);
        for (boolean[] linha : atingida) Arrays.fill(linha, false);
        Arrays.fill(colocado, false);
        Arrays.fill(acertos, 0);
    }

    public boolean dentro(int l, int c) {
        return l >= 0 && c >= 0 && l < tamanho && c < tamanho;
    }

    // ===== Posicionamento =====

    public boolean cabe(int id, int l, int c, boolean horizontal) {
        if (id < 0 || id >= frota.length) return false;
        for (int k = 0; k < frota[id]; k++) {
            int nl = horizontal ? l : l + k;
            int nc = horizontal ? c + k : c;
            if (!dentro(nl, nc)) return false;
            if (navio[nl][nc] >= 0 && navio[nl][nc] != id) return false;
        }
        return true;
    }

    public boolean colocar(int id, int l, int c, boolean horizontal) {
        if (!cabe(id, l, c, horizontal)) return false;
        remover(id);
        for (int k = 0; k < frota[id]; k++) {
            int nl = horizontal ? l : l + k;
            int nc = horizontal ? c + k : c;
            navio[nl][nc] = id;
        }
        colocado[id] = true;
        return true;
    }

    public void remover(int id) {
        if (id < 0 || id >= frota.length) return;
        for (int l = 0; l < tamanho; l++)
            for (int c = 0; c < tamanho; c++)
                if (navio[l][c] == id) navio[l][c] = -1;
        colocado[id] = false;
    }

    public int navioEm(int l, int c) {
        return dentro(l, c) ? navio[l][c] : -1;
    }

    public boolean estaColocado(int id) { return colocado[id]; }

    public boolean todosColocados() {
        for (boolean b : colocado) if (!b) return false;
        return true;
    }

    public int proximoNaoColocado() {
        for (int i = 0; i < colocado.length; i++) if (!colocado[i]) return i;
        return -1;
    }

    public void posicionarAleatorio(Random r) {
        Integer[] ordem = new Integer[frota.length];
        for (int i = 0; i < ordem.length; i++) ordem[i] = i;
        Arrays.sort(ordem, (a, b) -> frota[b] - frota[a]);

        while (true) {
            limpar();
            boolean ok = true;
            for (int id : ordem) {
                boolean posicionado = false;
                for (int t = 0; t < 500 && !posicionado; t++) {
                    posicionado = colocar(id, r.nextInt(tamanho), r.nextInt(tamanho), r.nextBoolean());
                }
                if (!posicionado) { ok = false; break; }
            }
            if (ok) return;
        }
    }

    // ===== Tiros =====

    public ResultadoTiro atirar(int l, int c) {
        if (!dentro(l, c)) return ResultadoTiro.INVALIDO;
        if (atingida[l][c]) return ResultadoTiro.REPETIDO;
        atingida[l][c] = true;
        int id = navio[l][c];
        if (id < 0) return ResultadoTiro.AGUA;
        acertos[id]++;
        return acertos[id] == frota[id] ? ResultadoTiro.AFUNDOU : ResultadoTiro.ACERTO;
    }

    /**
     * Usado no modo multiplayer em rede para registrar o resultado de um tiro
     * no tabuleiro do oponente (que originalmente fica vazio na memoria local).
     */
    public void registrarTiroRemoto(int l, int c, ResultadoTiro res, int idNavio, String coordsNavioAfundado) {
        if (!dentro(l, c)) return;
        atingida[l][c] = true;
        if (res == ResultadoTiro.ACERTO || res == ResultadoTiro.AFUNDOU) {
            if (idNavio >= 0 && idNavio < frota.length) {
                navio[l][c] = idNavio;
                acertos[idNavio]++;
            }
        }
        if (res == ResultadoTiro.AFUNDOU && idNavio >= 0 && coordsNavioAfundado != null && !coordsNavioAfundado.isEmpty()) {
            acertos[idNavio] = frota[idNavio];
            colocado[idNavio] = true;
            for (String par : coordsNavioAfundado.split(";")) {
                String[] lc = par.split("-");
                int nl = Integer.parseInt(lc[0]);
                int nc = Integer.parseInt(lc[1]);
                navio[nl][nc] = idNavio;
                atingida[nl][nc] = true;
            }
        }
    }

    /** Retorna todas as coordenadas ocupadas por um navio no formato "l-c;l-c". */
    public String celulasDoNavio(int id) {
        if (id < 0 || id >= frota.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int l = 0; l < tamanho; l++) {
            for (int c = 0; c < tamanho; c++) {
                if (navio[l][c] == id) {
                    if (sb.length() > 0) sb.append(";");
                    sb.append(l).append("-").append(c);
                }
            }
        }
        return sb.toString();
    }

    public boolean foiAtingida(int l, int c) {
        return dentro(l, c) && atingida[l][c];
    }

    public boolean afundado(int id) {
        return id >= 0 && id < frota.length && acertos[id] >= frota[id];
    }

    public boolean todosAfundados() {
        for (int i = 0; i < frota.length; i++) if (!afundado(i)) return false;
        return true;
    }

    public int naviosRestantes() {
        int n = 0;
        for (int i = 0; i < frota.length; i++) if (!afundado(i)) n++;
        return n;
    }
}