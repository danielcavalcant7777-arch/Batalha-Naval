import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class IaBatalhaNaval {

    private final Random random = new Random();
    private final List<int[]> alvosPrioritarios = new ArrayList<>();
    private final boolean inteligente;

    public IaBatalhaNaval(boolean inteligente) {
        this.inteligente = inteligente;
    }

    public int[] escolherTiro(Tabuleiro tabuleiroAlvo) {
        int n = tabuleiroAlvo.tamanho();

        // 1. Se acertou um navio antes e esta no modo Dificil, tenta as celulas vizinhas
        while (inteligente && !alvosPrioritarios.isEmpty()) {
            int[] prox = alvosPrioritarios.remove(0);
            int l = prox[0];
            int c = prox[1];
            if (tabuleiroAlvo.dentro(l, c) && !tabuleiroAlvo.foiAtingida(l, c)) {
                return prox;
            }
        }

        // 2. Caso contrario, escolhe uma celula ainda nao atingida
        List<int[]> disponiveis = new ArrayList<>();
        for (int l = 0; l < n; l++) {
            for (int c = 0; c < n; c++) {
                if (!tabuleiroAlvo.foiAtingida(l, c)) {
                    disponiveis.add(new int[]{l, c});
                }
            }
        }

        if (disponiveis.isEmpty()) return new int[]{0, 0};
        return disponiveis.get(random.nextInt(disponiveis.size()));
    }

    /** Informa a IA o que aconteceu no tiro para ela ajustar a mira. */
    public void registrarResultado(int l, int c, Tabuleiro.ResultadoTiro resultado) {
        if (!inteligente) return;

        if (resultado == Tabuleiro.ResultadoTiro.AFUNDOU) {
            alvosPrioritarios.clear(); // Navio afundou, volta a procurar outros
        } else if (resultado == Tabuleiro.ResultadoTiro.ACERTO) {
            int[][] vizinhos = {
                {l - 1, c}, {l + 1, c}, {l, c - 1}, {l, c + 1}
            };
            List<int[]> lista = new ArrayList<>();
            Collections.addAll(lista, vizinhos);
            Collections.shuffle(lista, random);
            alvosPrioritarios.addAll(0, lista);
        }
    }
}