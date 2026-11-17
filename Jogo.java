/**
 * Regras de uma partida entre dois jogadores no mesmo computador.
 * Cada jogador posiciona a frota; depois eles se alternam atirando
 * no tabuleiro do outro (um tiro por vez). Vence quem afundar tudo primeiro.
 */
public class Jogo {

    public enum Fase { POSICIONANDO, BATALHA, FIM }

    private final String[] nomes;
    private final Tabuleiro[] tabuleiros;
    private final int[] tiros = new int[2];
    private final int[] acertos = new int[2];
    private Fase fase = Fase.POSICIONANDO;
    private int vez = 0;
    private int vencedor = -1;

    public Jogo(String nome1, String nome2, int tamanho, int[] frota) {
        nomes = new String[] { nome1, nome2 };
        tabuleiros = new Tabuleiro[] { new Tabuleiro(tamanho, frota), new Tabuleiro(tamanho, frota) };
    }

    public String nome(int jogador)           { return nomes[jogador]; }
    public Tabuleiro tabuleiro(int jogador)   { return tabuleiros[jogador]; }
    public Fase fase()                        { return fase; }
    public int vez()                          { return vez; }
    public int vencedor()                     { return vencedor; }
    public int tiros(int jogador)             { return tiros[jogador]; }
    public int acertos(int jogador)           { return acertos[jogador]; }

    /** Tabuleiro em que o jogador da vez atira. */
    public Tabuleiro alvoDaVez() { return tabuleiros[1 - vez]; }

    /** Comeca a batalha se os dois jogadores ja posicionaram tudo. */
    public boolean iniciarBatalha() {
        if (fase != Fase.POSICIONANDO) return false;
        if (!tabuleiros[0].todosColocados() || !tabuleiros[1].todosColocados()) return false;
        fase = Fase.BATALHA;
        vez = 0;
        return true;
    }

    public Tabuleiro.ResultadoTiro atirar(int l, int c) {
        if (fase != Fase.BATALHA) return Tabuleiro.ResultadoTiro.INVALIDO;
        Tabuleiro alvo = alvoDaVez();
        Tabuleiro.ResultadoTiro r = alvo.atirar(l, c);
        if (r == Tabuleiro.ResultadoTiro.REPETIDO || r == Tabuleiro.ResultadoTiro.INVALIDO) return r;

        tiros[vez]++;
        if (r != Tabuleiro.ResultadoTiro.AGUA) acertos[vez]++;
        if (alvo.todosAfundados()) {
            fase = Fase.FIM;
            vencedor = vez;
        }
        return r;
    }

    /** Passa a vez depois que o jogador viu o resultado do tiro. */
    public void passarVez() {
        if (fase == Fase.BATALHA) vez = 1 - vez;
    }
}
