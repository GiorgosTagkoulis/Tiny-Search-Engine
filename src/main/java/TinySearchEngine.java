
import java.util.HashMap;
import se.kth.id1020.TinySearchEngineBase;
import java.util.*;
import se.kth.id1020.util.*;

public class TinySearchEngine implements TinySearchEngineBase {

    HashMap<String, WordContainer> wordIndexing = new HashMap<String, WordContainer>();
    HashMap<Document, Integer> docWordCounting = new HashMap<Document, Integer>();

    public void preInserts()
    {
      return;
    }

    public void insert (Word word, Attributes attrbt) {
        String wordName = word.word;
        Document document = attrbt.document;

        boolean documentExists = docWordCounting.containsKey(document);
        if (documentExists) {
            docWordCounting.put(document, docWordCounting.get(document) + 1);
        }

        else {
            docWordCounting.put(document, 1);
        }
 
        boolean wordExists = wordIndexing.containsKey(wordName);

        if (wordExists) {
            WordContainer wrd = wordIndexing.get(wordName);
            wrd.add(document);
        }

        else {
            wordIndexing.put(wordName, new WordContainer(word,attrbt));
        }
    }  

    public void insert (Sentence sentence, Attributes attr) {
        //We insert all the words of each sentence
          for(Word word : sentence.getWords()) {
            insert(word,attr);  
        }
    }
 
    public void postInserts () {
        return ;
    }

    public List<Document> search(String query) {
            String[] q = query.split("\\s+");           // The input from the user
            int size = q.length;
            int orderby = query.lastIndexOf("orderby");     // -1 if it doesn't exist
            boolean ordered;
            String p; 

            if (orderby > 0 && "orderby".equals(q[size -3])) {
                //It caches all the subqueries, and then saves the resulting documents in the hash table with the return string (in infix) as key.
                p = parse(query.substring(0,orderby));
                ordered = true;
            }
           else {
                p = parse(query); 
                ordered = false;
            }

            WordContainer result = wordIndexing.get(p);
            if (result == null) {
                result = new WordContainer(); 
            }

            ArrayList<Document> results = result.get();     
            
            if (ordered) {
                String arg = q[size -2]; 
                boolean asc = true; 

                if ("desc".equals(q[size - 1])) {
                    asc = false;
                }
                Comparator temp = new DocComparator(arg, result, asc);
                  Collections.sort(results, temp);
            }

            return results;
    }

    public String infix(String query) {
            String[] q = query.split("\\s+");
            int size = q.length; 
            int orderby = query.lastIndexOf("orderby");
            boolean ordered;
            String p; 

            if (orderby > 0 && "orderby".equals(q[size -3])) {
                p = parse(query.substring(0,orderby));
                ordered = true;
            }
            else {
                p = parse(query);
                ordered = false;
            }

            if (ordered) {
                p = p + " " + query.substring(orderby, query.length()).toUpperCase();
            }
            return p;
    }

    private String parse(String query) {
        String[] q = query.split("\\s+"); 
        WordContainer result; 
        Deque<String> deque = new ArrayDeque<String>();
        for (int i = q.length - 1; i >= 0; i--) {
            String s = q[i];
            if (s.equals("+")) {
                String one  = deque.removeFirst();          // if nothing it returns null
                String two = deque.removeFirst();
                String argument = "(" + one + " + " + two + ")";
                String revArgument = "(" + two + " + " + one + ")";

                 if (wordIndexing.containsKey(argument)) {
                    deque.addFirst(argument);
                    continue;
                }

                 else if (wordIndexing.containsKey(revArgument)) {
                    deque.addFirst(revArgument);
                    continue; 
                }

                else {
                    result = wordIndexing.get(one);
                    if (result == null) {
                        result = new WordContainer(); 
                    }
                    result = result.intersection(wordIndexing.get(two));
                    wordIndexing.put(argument,result);
                    deque.addFirst(argument);
                }
            }
            else if (s.equals("-")) {
                String one  = deque.removeFirst();
                String two = deque.removeFirst();
                String argument = "(" + one + " - " + two + ")";

                if (wordIndexing.containsKey(argument)) {
                    deque.addFirst(argument);
                    continue;
                }

                else {
                    result = wordIndexing.get(one);
                    if (result == null) {
                        result = new WordContainer(); 
                    }
                    result = result.difference(wordIndexing.get(two));
                    wordIndexing.put(argument,result);
                    deque.addFirst(argument);
                }
            }
            else if (s.equals("|")) {
                String one  = deque.removeFirst();
                String two = deque.removeFirst();
                String argument  = "(" + one + " | "+  two + ")";
                String revArgument = "(" + two + " | " + one + ")";

                if (wordIndexing.containsKey(argument)) {
                    deque.addFirst(argument);
                    continue;
                }

                else if (wordIndexing.containsKey(revArgument)) {
                    deque.addFirst(revArgument);
                    continue; 
                }

                else {
                    result = wordIndexing.get(one);
                    if (result == null) {
                        result = new WordContainer(); 
                    }

                    result = result.union(wordIndexing.get(two));
                    wordIndexing.put(argument,result);
                    deque.addFirst(argument);
                }
            }
            else {
                    deque.addFirst(s); 
            }
        }
        return deque.removeFirst();
    }

    private class DocComparator implements Comparator <Document> {
        boolean asc;
        String arg;
        WordContainer wrd; 
        public DocComparator(String arg, WordContainer wrd, boolean asc) {
            this.arg = arg;
            this.wrd = wrd; 
            this.asc = asc;
        }
 
        private int popularity(Document doc1, Document doc2) {

            int diff = doc1.popularity - doc2.popularity;
            diff = asc ? diff : -diff;
 
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else
                return 0;
 
        }
 
        private int relevance(Document doc1, Document doc2) {
            int diff = wrd.getRelevance(doc1).compareTo(wrd.getRelevance(doc2));
            diff = asc ? diff : -diff;
 
            if (diff > 0)
                return 1;
            else if (diff < 0)
                return -1;
            else 
                return 0;
        }
        public int compare(Document doc1, Document doc2) {
            if (arg.equals("popularity"))
                return popularity(doc1,doc2);
            else if (arg.equals("relevance"))
                return relevance(doc1,doc2);
            else
                return 0; 
        }
    }

     private class WordContainer {
        public boolean imported;
        public HashMap<Document, Double> relevance = new HashMap<Document, Double>();
        public ArrayList<Document> documents = new ArrayList<Document>(); 
 
        public WordContainer(Word word, Attributes attr) {
            imported = false;
            Document doc = attr.document; 
            relevance.put(doc,1.0);
            documents.add(doc); 
        }
 
        public WordContainer( WordContainer wrd) {
            this.relevance = new HashMap<>(wrd.relevance);
            this.documents = wrd.get(); 
            imported = true;
        }

        public WordContainer( ) {
            imported = true;
        }
 
        public void add(Document doc) {
            boolean documentExists = relevance.containsKey(doc);

            if (documentExists) {
                relevance.put(doc, relevance.get(doc) + 1);
            }

            else {
                relevance.put(doc,1.0);
                documents.add(doc); 
            }
        }

        
        public ArrayList<Document> get() {
            if (documents == null) 
                return new ArrayList<>();
            else
                return new ArrayList<>(documents);
        }
        
        private void addDocumentAndRelevance(Document doc, Double d) {
            if (!imported) {
                setRelevance(); 
            }
            relevance.put(doc, d);
            documents.add(doc); 
        }

        private void sumRelevance(Document doc, Double d) {
            if (!imported) {
                setRelevance(); 
            }
            relevance.put(doc, relevance.get(doc) + d);

        }

        private void setRelevance() {
            for (Document doc : documents) {
                double docTerm = docWordCounting.get(doc).doubleValue(); //The total number of terms (words)
                double termFreq = relevance.get(doc) / docTerm ;   //Shows how many times a term q appeared in a document d
                double totalSize = docWordCounting.size();
                double docsSize = documents.size(); 
                double invTermFreq = Math.log10(totalSize/docsSize);
                double res = termFreq * invTermFreq;
                relevance.put(doc, res); 
            }
            imported = true;
        }


        public Double getRelevance(Document doc) {
            if (!imported) {
                setRelevance(); 
            }
            return relevance.get(doc);
        }

        public boolean hasDocument(Document doc) {
            return relevance.containsKey(doc); 
        }

        public WordContainer intersection (WordContainer wrd) {
            WordContainer result = new WordContainer();
            if (wrd == null) {
                wrd = new WordContainer(); 
            }

            for (Document doc: wrd.get()) {
                if (this.hasDocument(doc)) {
                    result.addDocumentAndRelevance(doc, wrd.getRelevance(doc));
                    result.sumRelevance(doc, this.getRelevance(doc)); 
                }
            }
            return result; 
        }

        public WordContainer difference(WordContainer wrd) {
            WordContainer result = new WordContainer();
            if (wrd == null) {
                wrd = new WordContainer(); 
            }

            for (Document doc : this.get()) {
                if (!wrd.hasDocument(doc)) {
                    result.addDocumentAndRelevance(doc, this.getRelevance(doc));
                }
            }
            return result; 
            
        }
        public WordContainer union(WordContainer wrd) {
            if (!imported) {
                this.setRelevance(); 
            }

            WordContainer result = new WordContainer(this); 
            if (wrd == null) {
                wrd = new WordContainer(); 
            }
            for(Document document: wrd.get()){
                if (result.hasDocument(document)) {
                    result.sumRelevance(document, result.getRelevance(document));
                }
                else {
                    result.addDocumentAndRelevance(document, wrd.getRelevance(document));
                }
            }
            return result;
        }
    }
}