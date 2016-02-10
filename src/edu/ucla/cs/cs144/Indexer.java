package edu.ucla.cs.cs144;

import java.io.IOException;
import java.io.StringReader;
import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {

    private Connection conn = null;
    private IndexWriter indexWriter = null;

    /** Creates a new instance of Indexer */
    public Indexer() {
    }

    private IndexWriter getIndexWriter() throws IOException {
        if (indexWriter == null) {
            Directory indexDir = FSDirectory.open(new File("/var/lib/lucene/index/"));
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_2, new StandardAnalyzer());
            indexWriter = new IndexWriter(indexDir, config);
        }
        return indexWriter;
    }

    private void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
    }

    public void rebuildIndexes() throws Exception {
        // create a connection to the database to retrieve Items from MySQL
        try {
            conn = DbManager.getConnection(true);
        } catch (SQLException ex) {
            System.out.println(ex);
        }

        // initialize index
        indexWriter = getIndexWriter();

        // queries
        String itemID, name, description;
        PreparedStatement preparedStmt = conn.prepareStatement("select Category from ItemCategory where ItemID = ?");
        ResultSet items = selectFromItems();
        while (items.next()) {
            itemID = items.getString("ItemID");
            name = items.getString("Name");
            description = items.getString("Description");

            preparedStmt.setString(1, itemID);
            ResultSet categories = preparedStmt.executeQuery();
            String searchField = formatSearchField(name, description, categories);
            indexItem(itemID, name, searchField);
        }

        // close the database connection
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }

        // close index writer
        try {
            closeIndexWriter();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    private ResultSet selectFromItems() throws Exception {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery("select ItemID, Name, Description from Items");
    }

    private String formatSearchField(String name, String description, ResultSet categories) throws Exception {
        String searchField = name + " ";
        while (categories.next()) {
            searchField += categories.getString("Category") + " ";
        }
        searchField += description;
        return searchField;
    }

    private void indexItem(String itemID, String name, String searchField) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("ItemID", itemID, Field.Store.YES));
        doc.add(new StringField("Name", name, Field.Store.YES));
        doc.add(new TextField("searchField", searchField, Field.Store.NO));
        indexWriter.addDocument(doc);
    }

    public static void main(String args[]) throws Exception {
        Indexer idx = new Indexer();
        idx.rebuildIndexes();
    }   
}
