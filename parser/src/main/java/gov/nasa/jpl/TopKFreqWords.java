package gov.nasa.jpl;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

/**
 * Created by karanjeetsingh on 10/7/16.
 */
public class TopKFreqWords {

    public final static int K = 20;
    public final static String STOP_WORDS_FILE = "stop-words.txt";
    private Set<String> stopWords = new HashSet<String>();

    public TopKFreqWords() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                TopKFreqWords.class.getClassLoader().getResourceAsStream(STOP_WORDS_FILE)));
        try {
            String word;
            while ((word = reader.readLine()) != null) {
                stopWords.add(word.trim().toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error reading the Stop Words file");
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    final class WordFreq implements Comparable<WordFreq> {
        String word;
        int freq;

        public WordFreq(final String word, final int freq) {
            this.word = word;
            this.freq = freq;
        }

        public int compareTo(final WordFreq other) {
            return Integer.compare(this.freq, other.freq);
        }
    }

    public String[] removeStopWords(String[] words) {
        final List<String> filteredWords = new ArrayList<String>();
        for (final String word: words) {
            if (!stopWords.contains(word) && StringUtils.isAlpha(word)) {
                filteredWords.add(word);
            }
        }
        return filteredWords.toArray(new String[filteredWords.size()]);
    }

    public List<String> find(InputStream is, int k) {
        final Map<String, Integer> freqMap = new HashMap<String, Integer>();
        final PriorityQueue<WordFreq> minHeap = new PriorityQueue<WordFreq>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final List<String> result = new ArrayList<String>();
        String line;
        try {
            // Reading the stream
            while ((line = reader.readLine()) != null) {
                // Normalization and Filtering of words
                String[] words = line.toLowerCase().trim().split(" ");
                words = removeStopWords(words);
                for (final String word: words) {
                    int freq = 1;
                    if (freqMap.containsKey(word)) {
                        freq = freqMap.get(word) + 1;
                    }

                    // Add/Update the frequency map
                    freqMap.put(word, freq);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IOException while processing the file");
        } finally {
            IOUtils.closeQuietly(reader);
        }

        // Building the Heap
        for (Map.Entry<String, Integer> entry: freqMap.entrySet()) {
            if (minHeap.size() <= k) {
                minHeap.add(new WordFreq(entry.getKey(), entry.getValue()));
            } else if (entry.getValue() > minHeap.peek().freq) {
                minHeap.remove();
                minHeap.add(new WordFreq(entry.getKey(), entry.getValue()));
            }
        }

        // Extracting the Top K
        while (!minHeap.isEmpty()) {
            WordFreq wordFreq = minHeap.remove();
            result.add(wordFreq.word);
        }

        return Lists.reverse(result);
    }

    public static void main (String[] args) {
        TopKFreqWords topKFreqWords = new TopKFreqWords();
        String inputPath = args[0];
        InputStream is = null;
        List<String> result;
        try {
            is = new FileInputStream(new File(inputPath));
            result = topKFreqWords.find(is, K);
            for (String word: result) {
                System.out.println(word);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error Reading the Input Stream");
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
