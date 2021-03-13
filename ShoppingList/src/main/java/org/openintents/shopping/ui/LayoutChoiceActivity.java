package org.openintents.shopping.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.openintents.shopping.R;

public class LayoutChoiceActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    public static boolean show(Activity context) {
        if (PreferenceActivity.getShowLayoutChoice(context) && PreferenceActivity.getUsingHoloSearchFromPrefs(context)) {
            context.startActivity(new Intent(context, LayoutChoiceActivity.class));
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_layout_choice);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.layout_choice);

        if (PreferenceActivity.getUsingHoloSearchFromPrefs(this)) {
            radioGroup.check(R.id.layout_choice_actionbar);
        } else {
            radioGroup.check(R.id.layout_choice_bottom);
        }
        radioGroup.setOnCheckedChangeListener(this);

        findViewById(R.id.image_actionbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioGroup.check(R.id.layout_choice_actionbar);
            }
        });
        findViewById(R.id.image_bottom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioGroup.check(R.id.layout_choice_bottom);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        PreferenceActivity.setUsingHoloSearch(this, checkedId == R.id.layout_choice_actionbar);
        PreferenceActivity.setShowLayoutChoice(this, false);
        startActivity(new Intent(this, org.openintents.shopping.ShoppingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
