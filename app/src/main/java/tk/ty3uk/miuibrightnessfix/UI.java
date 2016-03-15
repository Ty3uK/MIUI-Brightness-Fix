package tk.ty3uk.miuibrightnessfix;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UI extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui);

        final SharedPreferences pref = getSharedPreferences("levels", MODE_WORLD_READABLE);
        final SharedPreferences.Editor editor = pref.edit();

        final EditText autoBrightnessLevels = (EditText) findViewById(R.id.auto_brightness_levels_edit);
        final EditText autoBrightnessLcdBacklightValues = (EditText) findViewById(R.id.auto_brightness_lcd_backlight_values_edit);

        int[] autoBrightnessLevelsArray = getResources().getIntArray(R.array.config_autoBrightnessLevels);
        int[] autoBrightnessLcdBacklightValuesArray = getResources().getIntArray(R.array.config_autoBrightnessLcdBacklightValues);

        if (pref.contains("autoBrightnessLevels") && pref.contains("autoBrightnessLcdBacklightValues"))
            try {
                autoBrightnessLevelsArray = Util.StringToIntArray(
                        pref.getString("autoBrightnessLevels", "")
                );
                autoBrightnessLcdBacklightValuesArray = Util.StringToIntArray(
                        pref.getString("autoBrightnessLcdBacklightValues", "")
                );
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }


        autoBrightnessLevels.setText(Util.IntArrayToString(autoBrightnessLevelsArray));
        autoBrightnessLcdBacklightValues.setText(Util.IntArrayToString(autoBrightnessLcdBacklightValuesArray));

        Button save = (Button) findViewById(R.id.save);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Util.StringToIntArray(
                            autoBrightnessLevels.getText().toString()
                    );
                    Util.StringToIntArray(
                            autoBrightnessLcdBacklightValues.getText().toString()
                    );

                    editor.putString(
                            "autoBrightnessLevels",
                            autoBrightnessLevels.getText().toString()
                    );
                    editor.putString(
                            "autoBrightnessLcdBacklightValues",
                            autoBrightnessLcdBacklightValues.getText().toString()
                    );
                    editor.commit();

                    Toast.makeText(getApplicationContext(), R.string.save_success, Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException nfe) {
                    Toast.makeText(getApplicationContext(), R.string.parsing_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
