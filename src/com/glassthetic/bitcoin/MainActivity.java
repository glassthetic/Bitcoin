package com.glassthetic.bitcoin;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends Activity {
  
  /**
   * Enum type representing a Bitcoin exchange
   */
  private enum mExchange {
    Bitstamp,
    MtGox
  }
  
  /**
   * Text-to-speech component
   */
  private TextToSpeech mTTS;
  
  /**
   * {@link CardScrollAdapter} that manages {@link #mCards}
   */
  private BitcoinCardScrollAdapter mAdapter;
  
  /**
   * Used by {@link #mAdapter}
   */
  private List<Card> mCards;
  
  /**
   * {@link CardScrollView} that is rendered onto the screen
   */
  private CardScrollView mCardScrollView;
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Initialize Text-to-speech component
    mTTS = new TextToSpeech(this, new OnInitListener() {
      @Override
      public void onInit(int status) {}
    });
    
    // Initialize set of cards
    createCards();
    
    // Initialize scroll view and adapter
    mCardScrollView = new CardScrollView(this);
    mAdapter = new BitcoinCardScrollAdapter();
    mCardScrollView.setAdapter(mAdapter);
    mCardScrollView.activate();
    
    // Show scroll view onto screen
    setContentView(mCardScrollView);
    
    // Begin async fetch of exchange data
    new GetExchangeTask().execute(mExchange.values());
  }
  
  /**
   * The app is about to be destroyed, do some cleanup.
   */
  @Override
  protected void onDestroy() {
    super.onDestroy();
    
    // Recycle TTS resources
    if (mTTS != null) {
      mTTS.stop();
      mTTS.shutdown();
    }
  }
  
  /**
   * Initializes {@link #mCards} with initial set of cards,
   * one for each {@link mExchange}
   */
  private void createCards() {
    mCards = new ArrayList<Card>();
    
    Card card;
    
    for (mExchange exchange : mExchange.values()) {
      card = new Card(this);
      card.setText("Loading");
      card.setInfo(exchange.toString());
      mCards.add(card);
    }
  }
  
  /**
   * Adapter for managing underlying List of cards
   * 
   * @see https://developers.google.com/glass/develop/gdk/reference/com/google/android/glass/widget/CardScrollAdapter
   */
  private class BitcoinCardScrollAdapter extends CardScrollAdapter {
    @Override
    public int findIdPosition(Object id) {
      return -1;
    }

    @Override
    public int findItemPosition(Object item) {
      return mCards.indexOf(item);
    }

    @Override
    public int getCount() {
      return mCards.size();
    }

    @Override
    public Card getItem(int position) {
      return mCards.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      return mCards.get(position).toView();
    }
    
    /**
     * Custom getter to retrieve {@link Card} by {@link mExchange}.
     * <br>
     * Similar in functionality to {@link #getItem(int)}
     * 
     * @param exchange {@link mExchange}
     * @return {@link Card}
     */
    public Card getItemByExchange(mExchange exchange) {
      Card item = null;
      
      for (Card card : mCards) {
        if (card.getInfo() == exchange.toString()) {
          item = card;
        }
      }
      
      return item;
    }
  }
  
  /**
   * Custom class used by {@link GetExchangeTask} to wrap an
   * {@link mExchange} and response values into a single object
   *
   */
  private class Result {
    mExchange exchange;
    String displayValue, speechValue;
    
    public Result(mExchange exchange, String displayValue, String speechValue) {
      this.exchange = exchange;
      this.displayValue = displayValue;
      this.speechValue = speechValue;
    }
  }
  
  /**
   * {@link AsyncTask} that fetches the Bitcoin value for a given {@link mExchange}
   * and updates {@link Card} text.
   * <br>
   * This task takes an variable argument of {@link mExchange} and returns a
   * {@link List} of {@link Result} objects.
   * 
   * @see http://developer.android.com/reference/android/os/AsyncTask.html
   */
  private class GetExchangeTask extends AsyncTask<mExchange, Void, List<Result>> {  
    /**
     * Runs async in a background thread.
     * <br>
     * The actual request and JSON parsing happens here.
     * <br>
     * Results will be passed to {@link #onPostExecute(List)}
     * 
     * @param params {@link mExchange}...
     * @return results
     */
    @Override
    protected List<Result> doInBackground(mExchange... params) {
      List<Result> results = new ArrayList<Result>();
      DecimalFormat twoPlaces = new DecimalFormat("$###.##");
      DecimalFormat whole = new DecimalFormat("$###");
      
      for (mExchange exchange : params) {
        String value = null, url = null;
        
        // Determine request url based on exchange
        switch (exchange) {
          case Bitstamp:
            url = "https://www.bitstamp.net/api/ticker/";
            break;
          case MtGox:
            url = "https://data.mtgox.com/api/2/BTCUSD/money/ticker_fast";
            break;
        }
        
        // Make request and attempt to parse JSON
        JSONObject json = null;
        try {
          json = Utils.getJson(url);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        // Extract value from JSON based on exchange JSON format
        switch (exchange) {
          case Bitstamp:
            try {
              value = json.getString("last");
            } catch (JSONException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            break;
          case MtGox:
            try {
              value = json.getJSONObject("data").getJSONObject("last").getString("value");
            } catch (JSONException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            break;
        }
        
        // Format values for display and speech
        float parsedValue = Float.parseFloat(value);
        String displayValue = twoPlaces.format(parsedValue);
        String speechValue = whole.format(parsedValue) + " on " + exchange;
        
        // Append Result to results List
        results.add(new Result(exchange, displayValue, speechValue));
      }     
      return results;
    }
    
    /**
     * Runs in the UI thread after {@link #doInBackground(mExchange...)}
     * 
     * @param results List of {@link Result} objects
     */
    @Override
    protected void onPostExecute(List<Result> results) {
      Boolean firstCard = true;
      for (Result result : results) {        
        // Update Card text with value
        Card card = mAdapter.getItemByExchange(result.exchange);
        card.setText(result.displayValue);
                
        // Speak the first card
        if (firstCard) {
          mTTS.speak(result.speechValue, TextToSpeech.QUEUE_ADD, null);
          firstCard = false;
        }        
      }
      
      // Notify view to refresh after data changes
      mCardScrollView.updateViews(true);
    }    
  }
}
