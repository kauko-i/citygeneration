package rakenteet;

import java.util.Random;


/**
 * Fibonacci-keko. Varoitusten mukaisesti ja teorioista huolimatta tämä ei ollut mikään suoritusajan pelastaja, 
 * mutta tulipahan tämäkin tehtyä.
 * Alkiot ovat ei-negatiivisia kokonaislukuja, koska viitteet niiden välillä voi toteuttaa pelkillä taulukoilla.
 * @author Ilari Kauko
 */
public class FibonacciKeko {
	
	
	// rajapinta lambdafunktiolle
	public static interface Int2Double {
		public double f(int i);
	}
	
	
	private int min, koko;
	private double minArvo;
	private Int2Double prioriteetti;
	private int[] ylempi, asteet, next, lapsi;
	private boolean[] loser;
	
	
	/**
	 * Lisää kekoon uuden alkion.
	 * @param alkio alkiota vastaava kokonaisluku
	 */
	public void lisaa(int alkio) {
		lisaa(alkio, true);
	}
	
	
	/**
	 * Lisää keon juurisolmuihin uuden solmun.
	 * @param alkio solmua vastaava kokonaisluku
	 * @param uusi onko solmu vasta tulossa rakenteeseen vai lapsisolmuista ylennetty
	 */
	public void lisaa(int alkio, boolean uusi) {
		double arvo = prioriteetti.f(alkio);
		if (min == -1 || arvo < minArvo) {
			int vanhaMin = min;
			min = alkio;
			next[min] = vanhaMin;
			minArvo = arvo;
		} else {
			int vanha2Min = next[min];
			next[min] = alkio;
			next[alkio] = vanha2Min;
		}
		loser[alkio] = false;
		ylempi[alkio] = -1;
		if (uusi) koko++;
	}
	
	
	/**
	 * Konstruktori.
	 * @param prioriteetti funktio kokonaisluvuilta reaaliluvuille, joka määrää lukujen vertailuperusteen
	 * @param max mahdollisesti tallennettavien kokonaislukujen yläraja + 1, taulukoiden koon määräämiseksi
	 */
	public FibonacciKeko(Int2Double prioriteetti, int max) {
		this.prioriteetti = prioriteetti;
		this.ylempi = new int[max];
		this.next = new int[max];
		this.lapsi = new int[max];
		this.asteet = new int[max];
		this.loser = new boolean[max];
		for (int i = 0; i < max; i++) {
			ylempi[i] = -1;
			next[i] = -1;
			lapsi[i] = -1;
		}
		this.min = -1;
	}
	
	
	/**
	 * Hakee, poistaa ja palauttaa keosta arvoltaan pienimmän kokonaisluvun.
	 * @return arvoltaan pienin kokonaisluku
	 */
	public int pienin() {
		if (min == -1) return -1;
		int palaute = min;
		int vanha2Min = next[min];
		if (lapsi[min] != -1) {
			min = lapsi[min];
			int k = min;
			while (next[k] != -1) {
				ylempi[k] = -1;
				k = next[k];
			}
			ylempi[k] = -1;
			next[k] = vanha2Min;
		} else min = vanha2Min;
		ylempi[palaute] = -1;
		next[palaute] = -1;
		lapsi[palaute] = -1;
		asteet[palaute] = 0;
		
		while (true) {
			boolean muutettu = false;
			for (int i = min; i != -1 && !muutettu; i = next[i]) {
				double p = prioriteetti.f(i);
				int vanhaJ = -1;
				for (int j = min; j != -1 && !muutettu; j = next[j]) {
					if (asteet[i] == asteet[j] && p < prioriteetti.f(j)) {
						int vanhaLapsi = lapsi[i];
						lapsi[i] = j;
						asteet[i]++;
						if (vanhaJ != -1) next[vanhaJ] = next[j];
						else min = next[j];
						next[j] = vanhaLapsi;
						for (int k = j; k != -1; k = next[k]) ylempi[k] = i;
						muutettu = true;
					}
					vanhaJ = j;
				}
			}
			if (!muutettu) break;
		}
		
		if (--koko == 0) min = -1;
		else {
			minArvo = prioriteetti.f(min);
			int vanhaMin = min;
			int ennenMinia = -1;
			int k = -1;
			for (int i = min; i != -1; i = next[i]) {
				double arvo = prioriteetti.f(i);
				if (arvo < minArvo) {
					min = i;
					minArvo = arvo;
					ennenMinia = k;
				}
				k = i;
			}
			if (ennenMinia != -1) {
				next[ennenMinia] = next[min];
				next[min] = vanhaMin;
			}
		}
		return palaute;
	}
	
	
	/**
	 * Siirtää tarvittaessa sellaisen solmun paikkaa rakenteessa, jonka arvon epäillään laskeneen.
	 * @param alkio solmua vastaava kokonaisluku
	 */
	public void laske(int alkio) {
		if (ylempi[alkio] == -1 && minArvo < prioriteetti.f(alkio) || ylempi[alkio] != -1 && prioriteetti.f(ylempi[alkio]) <= prioriteetti.f(alkio)) return;
		int i = ylempi[alkio];
		nostaJuuriin(alkio);
		if (i == -1) return;
		if (!loser[i]) loser[i] = true;
		else {
			while (ylempi[i] != -1 && loser[i]) {
				int j = ylempi[i];
				nostaJuuriin(i);
				i = j;
			}
			loser[i] = true;
		}
	}
	
	
	/**
	 * Nostaa juurisolmuihin solmun ja mahdollisesti ylentää sen minimisolmuksi lisää-operaation kautta.
	 * @param s nostettavaa solmua vastaava kokonaisluku
	 */
	public void nostaJuuriin(int s) {
		int vasenSisar;
		if (ylempi[s] == -1) vasenSisar = min;
		else {
			vasenSisar = lapsi[ylempi[s]];
			asteet[ylempi[s]]--;
		}
		if (vasenSisar != s) {
			while (next[vasenSisar] != s) vasenSisar = next[vasenSisar];
			next[vasenSisar] = next[s];
		} else if (s == min) min = next[s];
		else lapsi[ylempi[s]] = next[lapsi[ylempi[s]]];
		lisaa(s, false);
	}
	
	
	/**
	 * @return keon koko
	 */
	public int size() {
		return koko;
	}
	
	
	/**
	 * Tämä tarkitusohjelma saattaa huomata virheen keon totetuksessa.
	 * @return kokonaisluku, joka virheellisesti ei ole vanhempansa lapsi tai -1, jos tällaista virhekohtaa ei ole
	 */
	public int tarkista() {
		for (int i = 0; i < ylempi.length; i++) {
			if (ylempi[i] != -1) {
				int s = lapsi[ylempi[i]];
				while (s != i && s != -1) s = next[s];
				if (s == -1) return i;
			}
		}
		return -1;
	}
	
	
	/**
	 * Testiohjelma tälle kekorakenteelle. Kekorakenteen alkiot ovat satunnaisia kokonaislukuja väliltä 0–9999, 
	 * ja näiden prioriteetit satunnaisia reaalilukuja väliltä 0–1.
	 * @param args ei käytössä
	 */
	public static void main(String[] args) {
		int n = 10000;
		double[] arvot = new double[n];
		for (int i = 0; i < n; i++) arvot[i] = Math.random();
		boolean[] keossa = new boolean[n];
		Random r = new Random();
		FibonacciKeko keko = new FibonacciKeko(i -> arvot[i], n);
		for (int i = 0; i < n; i++) {
			double rand = Math.random();
			if (keko.size() < n/5 || rand < 1.0/3) {
				System.out.println("Lisätään uusi, satunnainen alkio.");
				int x = r.nextInt(n);
				while (keossa[x]) x = r.nextInt(n);
				keko.lisaa(x);
				keossa[x] = true;
			} else if (rand < 1.0/3*2) {
				System.out.println("Lasketaan satunnaisen alkion arvoa.");
				int x = r.nextInt(n);
				while (!keossa[x]) x = r.nextInt(n);
				arvot[x] *= Math.random();
				keko.laske(x);
			} else {
				System.out.println("Poistetaan pienin alkio.");
				keossa[keko.pienin()] = false;
			}
			System.out.println(keko.tarkista());
		}
	}
}
