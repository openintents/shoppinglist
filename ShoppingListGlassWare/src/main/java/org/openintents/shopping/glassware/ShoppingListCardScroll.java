package org.openintents.shopping.glassware;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.app.Card;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListCardScroll extends Activity {

    private List<Card> mCards;
    private CardScrollView mCardScrollView;
    private ShoppingListCardScrollAdapter adapter;

    private TimelineManager timelineManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createTitleCard();
        initCardsUI();
        initTimelineManager();

        setContentView(mCardScrollView);

        initListFromIntent();

    }

    private void initListFromIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            createCards(intent.getData());
        }
    }

    private void initTimelineManager() {
        timelineManager = TimelineManager.from(this);
    }

    private void createTitleCard() {
        Card card = new Card(this);
        card.setImageLayout(Card.ImageLayout.FULL);
        card.addImage(R.drawable.ic_mrt_bg);
        card.setText("Shopping List");
        mCards.add(card);
    }

    private void initCardsUI() {
        adapter = new ShoppingListCardScrollAdapter();
        mCards = new ArrayList<Card>();
        mCardScrollView = new CardScrollView(this);
        mCardScrollView.setAdapter(adapter);
        mCardScrollView.activate();
    }

    private void createCards(Uri data) {

        //shoppinglistname?ids=           Html.escapeHtml(shoppingListName) + "?ids=" + TextUtils.join(",", sentItems)

        String idString = data.getQueryParameter("ids");

        String[] ids = idString.split(",");

        for (int i = 0; i < ids.length; i++){
            Card card = timelineManager.query(Long.valueOf(ids[i]));

        }




        Card card = new Card(this);
        card.setText(data.toString());
        mCards.add(card);
    }

    private class ShoppingListCardScrollAdapter extends CardScrollAdapter {
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
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).toView();
        }

    }
}
