package rakenteet;

import java.util.Random;


/**
 * Parituskeko (englanniksi pairing heap). Tämä vaikutti aika tarkkaan yhtä nopealta kuin binäärikeko käytännössä.
 * Alkiot ovat ei-negatiivisia kokonaislukuja, koska viitteet niiden välillä voi toteuttaa pelkillä taulukoilla.
 * @author Ilari Kauko
 *
 */
public class Parituskeko {
	
	
	/**
	 * Rajapinta lambdafunktiolle.
	 * @author Ilari Kauko
	 */
	public static interface FInt2Double {
		public double f(int i);
	}
	
	private int[] lapsi, next, vanhempi;
	private FInt2Double prioriteetti;
	private int koko, min;
	
	
	/**
	 * Konstruktori
	 * @param max mahdollisesti tallennettavien kokonaislukujen yläraja + 1
	 * @param prioriteetti funktio kokonaisluvuilta reaaliluvuille, jotka määräävät kokonaislukujen vertailuperusteen
	 */
	public Parituskeko(int max, FInt2Double prioriteetti) {
		lapsi = new int[max];
		next = new int[max];
		vanhempi = new int[max];
		for (int i = 0; i < max; i++) {
			lapsi[i] = -1;
			next[i] = -1;
			vanhempi[i] = -1;
		}
		this.prioriteetti = prioriteetti;
		min = -1;
	}
	
	
	/**
	 * Lisää luvun toisen luvun lapseksi.
	 * @param vanhempi minkä lapseksi lisätään
	 * @param lapsi mikä lisätään lapseksi
	 */
	public void lisaaLapsi(int vanhempi, int lapsi) {
		next[lapsi] = this.lapsi[vanhempi];
		this.vanhempi[lapsi] = vanhempi;
		this.lapsi[vanhempi] = lapsi;
	}
	
	
	/**
	 * Yhdistää kaksi kekoa yhdeksi.
	 * @param a toisen keon juurisolmu
	 * @param b toisen keon juurisolmu
	 * @return syntyvän keon juurisolmu
	 */
	public int yhdista(int a, int b) {
		if (a == -1) return b;
		if (b == -1) return a;
		if (prioriteetti.f(a) < prioriteetti.f(b)) {
			lisaaLapsi(a, b);
			return a;
		}
		lisaaLapsi(b, a);
		return b;
	}
	
	
	/**
	 * Lisää kekoon uuden luvun.
	 * @param x lisättävä luku
	 */
	public void lisaa(int x) {
		min = yhdista(min, x);
		koko++;
	}
	
	
	/**
	 * Muodostaa sisarusjoukosta uuden keon rekursiivisesti. Käytetään pienimmän poiston jälkeen.
	 * @param x sisarusjoukon ensimmäinen alkio
	 * @return syntyvän keon juurisolmu
	 */
	public int twoPassYhdista(int x) {
		if (x == -1 || next[x] == -1) return x;
		int a = x;
		int b = next[x];
		int c = next[b];
		next[a] = -1;
		next[b] = -1;
		vanhempi[a] = -1;
		vanhempi[b] = -1;
		return yhdista(yhdista(a, b), twoPassYhdista(c));
	}
	
	
	/**
	 * @return keon koko
	 */
	public int size() {
		return koko;
	}
	
	
	/**
	 * Hakee, poistaa ja palauttaa prioriteetiltaan pienimmän kokonaisluvun.
	 * @return pienin kokonaisluku tai -1, jos keko on tyhjä
	 */
	public int pienin() {
		if (koko == 0) return -1;
		int palaute = min;
		min = twoPassYhdista(lapsi[min]);
		koko--;
		vanhempi[palaute] = -1;
		next[palaute] = -1;
		lapsi[palaute] = -1;
		if (min != -1) vanhempi[min] = -1;
		return palaute;
	}
	
	
	/**
	 * Siirtää keossa sellaisen solmun paikkaa, jonka arvon epäillään laskeneen.
	 * @param x solmu, jonka arvon epäillään laskeneen
	 */
	public void laske(int x) {
		if (x == min || prioriteetti.f(vanhempi[x]) < prioriteetti.f(x)) return;
		int vasenSisar = lapsi[vanhempi[x]];
		while (vasenSisar != x && next[vasenSisar] != x) vasenSisar = next[vasenSisar];
		if (vasenSisar == x) lapsi[vanhempi[x]] = next[vasenSisar];
		else next[vasenSisar] = next[x];
		vanhempi[x] = -1;
		next[x] = -1;
		lisaa(x);
		koko--;
	}
	
	
	/**
	 * Pieni tarkistusohjelma, joka saattaa huomata virheen rakenteessa.
	 * @return solmu, joka virheellisesti ei ole "vanhempansa lapsi" tai -1, jos tällaista ei ole
	 */
	public int tarkista() {
		for (int i = 0; i < vanhempi.length; i++) {
			if (vanhempi[i] != -1) {
				int s = lapsi[vanhempi[i]];
				while (s != i && s != -1) s = next[s];
				if (s == -1) return i;
			}
		}
		return -1;
	}
	
	
	/**
	 * Testataan operaatioita satunnaisesti.
	 * @param args ei käytössä
	 */
	public static void main(String[] args) {
		int n = 1000;
		double[] arvot = new double[n];
		boolean[] keossa = new boolean[n];
		for (int i = 0; i < n; i++) arvot[i] = Math.random();
		Parituskeko keko = new Parituskeko(n, i -> arvot[i]);
		Random r = new Random();
		for (int i = 0; i < n; i++) {
			double rand = Math.random();
			int x = r.nextInt(n);
			if (keko.size() == 0 || rand < 1.0/3) {
				System.out.println("Uusi alkio");
				while (keossa[x]) x = r.nextInt(n);
				keko.lisaa(x);
				keossa[x] = true;
			} else if (rand < 1.0/3*2) {
				System.out.println("Laske alkio");
				while (!keossa[x]) x = r.nextInt(n);
				arvot[x] *= Math.random();
				keko.laske(x);
			} else {
				System.out.println("Poista alkio");
				keossa[keko.pienin()] = false;
			}
			System.out.println(keko.tarkista());
		}
	}
}
