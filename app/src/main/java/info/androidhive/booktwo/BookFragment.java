package info.androidhive.booktwo;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookFragment extends Fragment {

    // Declares the array adapter used
    ArrayAdapter<String> mBookAdapter;

    //Empty constructor
    public BookFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    //Creates the view used
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Instructions for the user when opening the app
        String[] booksArray = {
                "Enter a topic into the search box and press the button!"
        };

        //Makes the instructions list compatible
        List<String> bookList = new ArrayList<String>(
                Arrays.asList(booksArray)
        );

        //Creates the adapter
        mBookAdapter =
                new ArrayAdapter<String>(getActivity(),
                        R.layout.list_item_books,
                        R.id.list_item_book_textview,
                        bookList
                );

        //Creates the view used
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //Sets the adapter
        ListView listView = (ListView) rootView.findViewById(
                R.id.listview_books);
        listView.setAdapter(mBookAdapter);

        //Grabs a reference to the Button
        Button generate = (Button) rootView.findViewById(R.id.generate_button);

        //Creates an on click listener for the generate button
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Stores the text in the EditText box as a String
                EditText topicText = (EditText) getView().findViewById(R.id.topic_text);
                String topic = topicText.getText().toString();

                //Executes getting the data
                getBookData getData = new getBookData();
                getData.execute(topic);
            }
        });


        return rootView;
    }

    public class getBookData extends AsyncTask<String, Void, String[]> {

        //Method to get and parse the JSON data
        private String[] getBookDataFromJson(String bookJsonStr) throws JSONException{
            final String BOOK_ITEMS = "items";
            final String BOOK_INFO = "volumeInfo";
            final String BOOK_TITLE = "title";
            final String BOOK_AUTHORS = "authors";

            //Creates a main JSON object and array
            JSONObject bookJson = new JSONObject(bookJsonStr);
            JSONArray bookArray = bookJson.getJSONArray(BOOK_ITEMS);

            //Creates a String array list for results the same length as the JSON array
            String[] resultStrs = new String[bookArray.length()];

            //Loops through the main JSON array
            for(int i = 0; i < bookArray.length(); i++){

                //Gets the book title and creates a string to hold it
                JSONObject volumeInfo = bookArray.getJSONObject(i).getJSONObject(BOOK_INFO);
                String title = volumeInfo.getString(BOOK_TITLE);

                //This next part gave me a headache... but I learned a lot.
                //In some cases there were no authors listed so it got tricky.
                //First declare a new JSONArray
                JSONArray authors = new JSONArray();

                //Check to see if there is information in the authors section
                try {
                    authors = volumeInfo.getJSONArray(BOOK_AUTHORS);
                } catch (JSONException e){

                }

                //Creates a new array list to the length of the found authors plus one
                String[] authorArray = new String[authors.length() + 1];

                //If there are multiple authors I wanted commas to seperate them so
                //this loop happened
                if(authors.length() != 0 && authors.length() > 1){
                authorArray = new String[authors.length()];
                for(int j = 0; j < authors.length(); j++) {
                    authorArray[j] = authors.getString(j) + ", ";
                }}
                //If there were no Authors I put No Author in the Author spot
                else if (authors.length() == 0){
                    authorArray[0] = "No Author";
                }
                //If there was only one Author I put it in the adapter.
                else{
                    authorArray[0] = authors.getString(0);
                }

                //To convert the array list to a string I used a StringBuffer
                StringBuffer authorBuffer = new StringBuffer();
                for(int k = 0; k < authorArray.length; k++){
                    authorBuffer.append(authorArray[k]);
                }

                //authorString is the array list printed into a String
                String authorString = authorBuffer.toString();

                //Finally we have the list ready string ready to be put into the list.
                resultStrs[i] = "Book Title : " + title + "\nAuthor : " + authorString;
            }

            //Returns the whole array list of strings.
            return resultStrs;
        }

        @Override
        protected String[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String booksJsonStr = null;

            try {

                //I constructed the URL using a Uri builder
                final String BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
                final String QUERY_PARAM = "q";
                final String RESULTS_PARAM = "maxResults";

                //This is what build it.
                Uri builtUri = Uri.parse(BOOKS_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(RESULTS_PARAM, "20").build();

                //Set the url as a URL
                URL url = new URL(builtUri.toString());

                //Try to get a connection to the created url
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                booksJsonStr = buffer.toString();
            } catch (IOException e) {
                return null;

                //Clean up
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                    }
                }
            }

            try {
                return getBookDataFromJson(booksJsonStr);
            } catch (JSONException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result){

            //Data goes into the adapter!!
            if (result != null){
                mBookAdapter.clear();
                for (String bookInfoStr : result){
                    mBookAdapter.add(bookInfoStr);
                }
            }
        }
    }
}