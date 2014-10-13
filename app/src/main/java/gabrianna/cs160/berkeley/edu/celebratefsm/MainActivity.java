package gabrianna.cs160.berkeley.edu.celebratefsm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by Gabi on 10/11/2014.
 */
public class MainActivity extends Activity {
    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteDeckOfCards mRemoteDeckOfCards;
    private RemoteResourceStore mRemoteResourceStore;
    private CardImage[] mCardImages;
    private ToqBroadcastReceiver toqReceiver;

    public int randomNum;
    public static Random random = new Random();
    LocationManager locationManager;
    LocationListener locationListener;

    private static final double destinationLatitude =  37.86965;
    private static final double destinationLongitude =  -122.25914;

    private double lastLatitude;
    private double lastLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqReceiver = new ToqBroadcastReceiver();
        init();
        setupUI();
    }

    /**
     * @see android.app.Activity#onStart()
     * This is called after onCreate(Bundle) or after onRestart() if the activity has been stopped
     */
    protected void onStart(){
        super.onStart();

        Log.d(Constants.TAG, "ToqApiDemo.onStart");
        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupUI() {

        findViewById(R.id.install_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                install();
            }
        });

        findViewById(R.id.uninstall_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uninstall();
            }
        });

    }

    private void sendNotification(String name) {
        String[] message = new String[1];
        message[0] = name;
        // Create a NotificationTextCard
        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "Draw Request", message);

        // Draw divider between lines of text
        notificationCard.setShowDivider(true);
        // Vibrate to alert user when showing the notification
        notificationCard.setVibeAlert(true);
        // Create a notification with the NotificationTextCard we made
        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);
        locationManager.removeUpdates(locationListener);

        try {
            // Send the notification
            mDeckOfCardsManager.sendNotification(notification);
//            Toast.makeText(this, "Sent Notification", Toast.LENGTH_SHORT).show();
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
        }
    }
    /**
     * Installs applet to Toq watch if app is not yet installed
     */
    private void install() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (!isInstalled) {
            try {
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }

        try{
            storeDeckOfCards();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void uninstall() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (isInstalled) {
            try{
                mDeckOfCardsManager.uninstallDeckOfCards();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, getString(R.string.error_uninstalling_deck_of_cards), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.already_uninstalled), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Add image card to deck
     */
    private void addImageCard(Bitmap bitmap) {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        int currSize = listCard.size()+1;
        String cardID = "card.image." + Integer.toString(currSize);
        CardImage cardImage =  new CardImage(cardID, bitmap);
        mRemoteResourceStore.addResource(cardImage);

        SimpleTextCard newCard = new SimpleTextCard(Integer.toString(currSize));
        newCard.setHeaderText("View Drawing");
        newCard.setCardImage(mRemoteResourceStore, cardImage);
        listCard.add(newCard);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create image card.", Toast.LENGTH_SHORT).show();
        }
    }


    private void removeDeckOfCards() {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        if (listCard.size() == 0) {
            return;
        }

        int currSize = listCard.size();

        listCard.remove(currSize-1);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to delete Card from ListCard", Toast.LENGTH_SHORT).show();
        }

    }

    // Initialise
    private void init(){

        // Create the resource store for icons and images
        mRemoteResourceStore= new RemoteResourceStore();

        DeckOfCardsLauncherIcon whiteIcon = null;
        DeckOfCardsLauncherIcon colorIcon = null;

        // Get the launcher icons
        try{
            whiteIcon= new DeckOfCardsLauncherIcon("white.launcher.icon", getBitmap("bw.png"), DeckOfCardsLauncherIcon.WHITE);
            colorIcon= new DeckOfCardsLauncherIcon("color.launcher.icon", getBitmap("color.png"), DeckOfCardsLauncherIcon.COLOR);
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get launcher icon");
            return;
        }

        mCardImages = new CardImage[6];
        try{
            mCardImages[0]= new CardImage("card.image.1", getBitmap("art_goldberg_toq.png"));
            mCardImages[1]= new CardImage("card.image.2", getBitmap("jack_weinberg_toq.png"));
            mCardImages[2]= new CardImage("card.image.3", getBitmap("jackie_goldberg_toq.png"));
            mCardImages[3]= new CardImage("card.image.4", getBitmap("joan_baez_toq.png"));
            mCardImages[4]= new CardImage("card.image.5", getBitmap("mario_savio_toq.png"));
            mCardImages[5]= new CardImage("card.image.6", getBitmap("michael_rossman_toq.png"));
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get picture icon");
            return;
        }

        // Try to retrieve a stored deck of cards
        try {
            // If there is no stored deck of cards or it is unusable, then create new and store
            if ((mRemoteDeckOfCards = getStoredDeckOfCards()) == null){
                mRemoteDeckOfCards = createDeckOfCards();
                storeDeckOfCards();
            }
        }
        catch (Throwable th){
            th.printStackTrace();
            mRemoteDeckOfCards = null; // Reset to force recreate
        }

        // Make sure in usable state
        if (mRemoteDeckOfCards == null){
            mRemoteDeckOfCards = createDeckOfCards();
        }

        // Set the custom launcher icons, adding them to the resource store
        mRemoteDeckOfCards.setLauncherIcons(mRemoteResourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});

        // Re-populate the resource store with any card images being used by any of the cards
        for (Iterator<Card> it= mRemoteDeckOfCards.getListCard().iterator(); it.hasNext();){

            String cardImageId= ((SimpleTextCard)it.next()).getCardImageId();

            if ((cardImageId != null) && !mRemoteResourceStore.containsId(cardImageId)){

                if (cardImageId.equals("card.image.1")){
                    mRemoteResourceStore.addResource(mCardImages[0]);
                }
                else if (cardImageId.equals("card.image.2")){
                    mRemoteResourceStore.addResource(mCardImages[1]);
                }
                else if (cardImageId.equals("card.image.3")){
                    mRemoteResourceStore.addResource(mCardImages[2]);
                }
                else if (cardImageId.equals("card.image.4")){
                    mRemoteResourceStore.addResource(mCardImages[3]);
                }
                else if (cardImageId.equals("card.image.5")){
                    mRemoteResourceStore.addResource(mCardImages[4]);
                }
                else if (cardImageId.equals("card.image.6")){
                    mRemoteResourceStore.addResource(mCardImages[5]);
                }

            }
        }

        mDeckOfCardsManager.addDeckOfCardsEventListener(new DeckOfCardsEventListener(){
                public void onCardOpen(String cardId) {
                    Intent intent = new Intent(getApplicationContext(),
                            DrawingActivity.class);
                    startActivity(intent);
                }
            public void onCardVisible(String cardId) {}
            public void onCardInvisible(String cardId) {}
            public void onCardClosed(String cardId) {}
            public void onMenuOptionSelected(String cardId, String menuOption) {}
            public void onMenuOptionSelected(String cardId, String menuOption, String what) {}
        });

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                Location destination = new Location("destination");
                destination.setLatitude(destinationLatitude);
                destination.setLongitude(destinationLongitude);
                float difference = destination.distanceTo(location);

                if (difference < 50) {
                    randomNum = random.nextInt(6);
                    switch(randomNum) {
                        case 0:
                            sendNotification("Jack Weinberg");
                        case 1:
                            sendNotification("Joan Baez");
                        case 2:
                            sendNotification("Michael Rossman");
                        case 3:
                            sendNotification("Art Goldberg");
                        case 4:
                            sendNotification("Jackie Goldberg");
                        case 5:
                            sendNotification("Mario Savio");
                    }
                }
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100000, 0, locationListener);
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100000, 0, locationListener);
        }

    }

    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(String fileName) throws Exception{

        try{
            InputStream is= getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        }
        catch (Exception e){
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception{

        if (!isValidDeckOfCards()){
            Log.w(Constants.TAG, "Stored deck of cards not valid for this version of the demo, recreating...");
            return null;
        }

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr= prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null){
            return null;
        }
        else{
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }

    }

    /**
     * Uses SharedPreferences to store the deck of cards
     * This is mainly used to
     */
    private void storeDeckOfCards() throws Exception{
        // Retrieve and hold the contents of PREFS_FILE, or create one when you retrieve an editor (SharedPreferences.edit())
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Create new editor with preferences above
        SharedPreferences.Editor editor = prefs.edit();
        // Store an encoded string of the deck of cards with key DECK_OF_CARDS_KEY
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(mRemoteDeckOfCards));
        // Store the version code with key DECK_OF_CARDS_VERSION_KEY
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        // Commit these changes
        editor.commit();
    }

    // Check if the stored deck of cards is valid for this version of the demo
    private boolean isValidDeckOfCards(){

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Return 0 if DECK_OF_CARDS_VERSION_KEY isn't found
        int deckOfCardsVersion= prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        return deckOfCardsVersion >= Constants.VERSION_CODE;
    }

    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards(){

        ListCard listCard= new ListCard();

        // Create a SimpleTextCard with 1 + the current number of SimpleTextCards
        SimpleTextCard simpleTextCard = new SimpleTextCard(Integer.toString(1));
        simpleTextCard.setHeaderText("Art Goldberg");
        String[] messages = {"Draw \"Now\""};
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        simpleTextCard.setShowDivider(true);
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[0]);
        listCard.add(simpleTextCard);

        simpleTextCard = new SimpleTextCard(Integer.toString(2));
        simpleTextCard.setHeaderText("Jack Weinberg");
        messages[0] = "Draw \"FSM\"";
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        simpleTextCard.setShowDivider(true);
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[1]);
        listCard.add(simpleTextCard);

        simpleTextCard = new SimpleTextCard(Integer.toString(3));
        simpleTextCard.setHeaderText("Jackie Weinberg");
        messages[0] = "Draw \"SLATE\"";
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        simpleTextCard.setShowDivider(true);
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[2]);
        listCard.add(simpleTextCard);

        simpleTextCard = new SimpleTextCard(Integer.toString(4));
        simpleTextCard.setHeaderText("Joan Baez");
        messages[0] = "Draw a Megaphone";
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        simpleTextCard.setShowDivider(true);
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[3]);
        listCard.add(simpleTextCard);

        simpleTextCard = new SimpleTextCard(Integer.toString(5));
        simpleTextCard.setHeaderText("Mario Savio");
        messages[0] = "Express your own view of Free Speech in a drawing";
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        simpleTextCard.setShowDivider(true);
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[4]);
        listCard.add(simpleTextCard);

        simpleTextCard = new SimpleTextCard(Integer.toString(6));
        simpleTextCard.setHeaderText("Michael Rossman");
        messages[0] = "Draw \"Free Speech\"";
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        simpleTextCard.setShowDivider(true);
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[5]);
        listCard.add(simpleTextCard);

        return new RemoteDeckOfCards(this, listCard);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // this is very important, otherwise you would get a null Scheme in the
        // onResume later on.
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Bitmap drawing = getIntent().getParcelableExtra("drawing");
        if (drawing != null) {
            ListCard listCard = mRemoteDeckOfCards.getListCard();
            int currSize = listCard.size();
            if (currSize == 6) addImageCard(drawing);
            else {
                removeDeckOfCards();
                addImageCard(drawing);
            }
        }
    }
}
