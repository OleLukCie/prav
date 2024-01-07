package eu.siacs.conversations.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.net.Uri;

import androidx.appcompat.app.ActionBar;

import eu.siacs.conversations.R;

public class TosActivity extends XmppActivity {

    private static final int REQUEST_IMPORT_BACKUP = 0x63fb;

    @Override
    protected void refreshUiReal() {

    }

    @Override
    void onBackendConnected() {

    }

    @Override
    public void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            setIntent(intent);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tos);
        setSupportActionBar(findViewById(R.id.toolbar));
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
        }
        final Button agreeButton = findViewById(R.id.agree);
        final Button changeLangButton = findViewById(R.id.langchange);
        final Button importBackupButton = findViewById(R.id.action_import_backup);
        final TextView welcomeText = findViewById(R.id.welcome_text);
        agreeButton.setOnClickListener(v -> {
            final Intent intent = new Intent(this, EnterPhoneNumberActivity.class);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().putBoolean("tos", true).apply();
            addInviteUri(intent);
            startActivity(intent);
            finish();
        });
        changeLangButton.setOnClickListener(v -> {
    		// Specify the action and the settings page you want to open
    		Intent intent = new Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS);
    		intent.setData(Uri.parse("package:" + getPackageName()));
        	startActivity(intent);
        });

        importBackupButton.setOnClickListener(v -> {
            if (hasStoragePermission(REQUEST_IMPORT_BACKUP)) {
                startActivity(new Intent(this, ImportBackupActivity.class));
            }
        });
        welcomeText.setText(Html.fromHtml(getString(R.string.welcome_text_quicksy_static)));
        welcomeText.setMovementMethod(LinkMovementMethod.getInstance());

    }

    public void addInviteUri(Intent intent) {
        StartConversationActivity.addInviteUri(intent, getIntent());
    }
}
