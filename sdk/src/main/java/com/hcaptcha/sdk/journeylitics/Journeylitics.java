package com.hcaptcha.sdk.journeylitics;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.MainThread;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main entry
 * @SuppressWarnings("PMD.GodClass") - This class intentionally handles multiple responsibilities
 * for view instrumentation and event tracking
 * @SuppressWarnings("PMD.UseUtilityClass") - This class maintains static state and lifecycle
 */
@SuppressWarnings({"PMD.GodClass", "PMD.UseUtilityClass"})
public class Journeylitics {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static Application sApp;
    private static JLConfig sConfig = JLConfig.DEFAULT;
    private static final CopyOnWriteArrayList<JLSink> SINKS = new CopyOnWriteArrayList<>();
    private static final WeakHashMap<View, Boolean> INSTRUMENTED = new WeakHashMap<>();

    private static final Application.ActivityLifecycleCallbacks LIFECYCLE_CALLBACKS =
            new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityResumed(Activity activity) {
                    if (sConfig.isEnableScreens()) {
                        final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                                new AbstractMap.SimpleEntry<>(FieldKey.SCREEN,
                                        activity.getClass().getSimpleName()),
                                new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "appear")
                        );
                        emit(EventKind.screen, activity.getClass().getSimpleName(), meta);
                    }
                    // Install view hooks lazily when screen is visible
                    // Use a small delay to ensure the view hierarchy is fully established
                    final ViewGroup content = activity.findViewById(android.R.id.content);
                    if (content != null) {
                        content.post(new Runnable() {
                            @Override
                            public void run() {
                                instrumentActivityViews(activity);
                            }
                        });
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    if (sConfig.isEnableScreens()) {
                        final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                                new AbstractMap.SimpleEntry<>(FieldKey.SCREEN,
                                        activity.getClass().getSimpleName()),
                                new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "disappear")
                        );
                        emit(EventKind.screen, activity.getClass().getSimpleName(), meta);
                    }
                }

                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    // No-op: not needed for analytics
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    // No-op: not needed for analytics
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    // No-op: not needed for analytics
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    // No-op: not needed for analytics
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    // No-op: not needed for analytics
                }
            };

    @MainThread
    public static void start(Context context) {
        start(context, JLConfig.DEFAULT);
    }

    @MainThread
    public static void start(Context context, JLConfig configuration) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        final Context appCtx = context.getApplicationContext();
        if (!(appCtx instanceof Application)) {
            throw new IllegalArgumentException("context must be Application or provide applicationContext");
        }
        sApp = (Application) appCtx;
        sConfig = configuration;
        SINKS.clear();
        SINKS.addAll(configuration.getSinks());
        sApp.registerActivityLifecycleCallbacks(LIFECYCLE_CALLBACKS);
    }

    static void addSink(JLSink sink) {
        SINKS.add(sink);
    }

    static void removeSink(JLSink sink) {
        SINKS.remove(sink);
    }

    static void instrumentViews(Activity activity) {
        instrumentActivityViews(activity);
    }

    public static void emit(EventKind kind, String view, Map<String, Object> metadata) {
        final JLEvent event = new JLEvent(kind, view, new HashMap<>(metadata));
        for (JLSink sink : SINKS) {
            try {
                sink.emit(event);
            } catch (Exception e) {
                // Ignore sink errors
            }
        }
    }

    public static void emit(EventKind kind, String view) {
        emit(kind, view, new HashMap<>());
    }

    // --- View instrumentation -------------------------------------------------------------

    private static void instrumentActivityViews(Activity activity) {
        final ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        traverseAndHook(root);
    }

    private static void traverseAndHook(View view) {
        if (Boolean.TRUE.equals(INSTRUMENTED.put(view, true))) {
            return;
        }

        if (view instanceof Button) {
            hookClick(view);
        } else if (view instanceof ImageButton) {
            hookClick(view);
        } else if (view instanceof EditText) {
            hookTextInput((EditText) view);
        } else if (view instanceof AutoCompleteTextView) {
            hookTextInput((EditText) view);
        } else if (view instanceof MultiAutoCompleteTextView) {
            hookTextInput((EditText) view);
        } else if (view instanceof CheckedTextView) {
            hookCheckedTextView((CheckedTextView) view);
        } else if (view instanceof TextView && view.isClickable()) {
            hookClick(view);
        } else if (view instanceof CompoundButton) {
            hookToggle((CompoundButton) view);
        } else if (view instanceof SeekBar) {
            hookSeek((SeekBar) view);
        } else if (view instanceof SearchView) {
            hookSearch((SearchView) view);
        } else if (view instanceof ScrollView) {
            hookScrollView((ScrollView) view);
        } else if (view instanceof HorizontalScrollView) {
            hookHScrollView((HorizontalScrollView) view);
        }

        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                traverseAndHook(viewGroup.getChildAt(i));
            }
        }
    }

    private static String viewIdName(View view) {
        final int id = view.getId();
        if (id != View.NO_ID) {
            try {
                return view.getResources().getResourceEntryName(id);
            } catch (Exception e) {
                return String.valueOf(id);
            }
        }

        // Try to get meaningful identifier from various sources
        if (view instanceof EditText) {
            final EditText editText = (EditText) view;
            if (editText.getHint() != null && !editText.getHint().toString().isEmpty()) {
                return editText.getHint().toString();
            }
            if (editText.getTag() != null && !editText.getTag().toString().isEmpty()) {
                return editText.getTag().toString();
            }
            if (editText.getContentDescription() != null && !editText.getContentDescription().toString().isEmpty()) {
                return editText.getContentDescription().toString();
            }
        } else if (view instanceof TextView) {
            final TextView textView = (TextView) view;
            if (textView.getHint() != null && !textView.getHint().toString().isEmpty()) {
                return textView.getHint().toString();
            }
            if (textView.getTag() != null && !textView.getTag().toString().isEmpty()) {
                return textView.getTag().toString();
            }
            if (textView.getContentDescription() != null && !textView.getContentDescription().toString().isEmpty()) {
                return textView.getContentDescription().toString();
            }
        } else if (view instanceof Button) {
            final Button button = (Button) view;
            if (button.getText() != null && !button.getText().toString().isEmpty()) {
                return button.getText().toString();
            }
            if (button.getTag() != null && !button.getTag().toString().isEmpty()) {
                return button.getTag().toString();
            }
            if (button.getContentDescription() != null && !button.getContentDescription().toString().isEmpty()) {
                return button.getContentDescription().toString();
            }
        }

        // Fallback to class name with position if available
        final ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            final ViewGroup parentGroup = (ViewGroup) parent;
            final int index = parentGroup.indexOfChild(view);
            if (index >= 0) {
                return view.getClass().getSimpleName() + "_" + index;
            }
        }

        final int magicNumber = 1000;
        return view.getClass().getSimpleName() + "_" + (view.hashCode() % magicNumber);
    }

    private static void hookClick(View view) {
        final View.OnClickListener original = getOnClickListener(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clickedView) {
                if (original != null) {
                    original.onClick(clickedView);
                }
                if (sConfig.isEnableClicks()) {
                    final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                        new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(clickedView))
                    );
                    emit(EventKind.click, clickedView.getClass().getSimpleName(), meta);
                }
            }
        });
    }

    private static void hookCheckedTextView(CheckedTextView checkedTextView) {
        // CheckedTextView doesn't have an OnCheckedChangeListener like CompoundButton,
        // so we hook the click listener and track the checked state change
        final View.OnClickListener original = getOnClickListener(checkedTextView);
        checkedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (original != null) {
                    original.onClick(view);
                }
                if (sConfig.isEnableToggles() && view instanceof CheckedTextView) {
                    final CheckedTextView checkedView = (CheckedTextView) view;
                    // Post to get the state after the click has been processed
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            final boolean isChecked = checkedView.isChecked();
                            final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                                new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(checkedView)),
                                new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "toggle"),
                                new AbstractMap.SimpleEntry<>(FieldKey.VALUE, String.valueOf(isChecked))
                            );
                            emit(EventKind.click, checkedView.getClass().getSimpleName(), meta);
                        }
                    });
                }
            }
        });
    }

    private static void hookToggle(CompoundButton compoundButton) {
        final CompoundButton.OnCheckedChangeListener original = getOnCheckedChangeListener(compoundButton);
        compoundButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                if (original != null) {
                    original.onCheckedChanged(button, isChecked);
                }
                if (sConfig.isEnableToggles()) {
                    final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                        new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(button)),
                        new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "toggle"),
                        new AbstractMap.SimpleEntry<>(FieldKey.VALUE, String.valueOf(isChecked))
                    );
                    emit(EventKind.click, button.getClass().getSimpleName(), meta);
                }
            }
        });
    }

    private static void hookSeek(SeekBar seekBar) {
        final SeekBar.OnSeekBarChangeListener original = getOnSeekBarChangeListener(seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (original != null) {
                    original.onProgressChanged(bar, progress, fromUser);
                }
                if (fromUser && sConfig.isEnableSliders()) {
                    final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                        new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(seekBar)),
                        new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "change"),
                        new AbstractMap.SimpleEntry<>(FieldKey.VALUE, progress)
                    );
                    emit(EventKind.drag, seekBar.getClass().getSimpleName(), meta);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                if (original != null) {
                    original.onStartTrackingTouch(bar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (original != null) {
                    original.onStopTrackingTouch(bar);
                }
            }
        });
    }

    private static void hookSearch(SearchView searchView) {
        final SearchView.OnQueryTextListener original = getOnQueryTextListener(searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final boolean handled = original != null && original.onQueryTextSubmit(query);
                if (sConfig.isEnableSearch()) {
                    final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                        new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(searchView)),
                        new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "submit"),
                        new AbstractMap.SimpleEntry<>(FieldKey.VALUE,
                            query != null ? query.length() : 0)
                    );
                    emit(EventKind.click, searchView.getClass().getSimpleName(), meta);
                }
                return handled;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                final boolean handled = original != null && original.onQueryTextChange(newText);
                if (sConfig.isEnableSearch()) {
                    final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                        new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(searchView)),
                        new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "change")
                    );
                    emit(EventKind.edit, searchView.getClass().getSimpleName(), meta);
                }
                return handled;
            }
        });
    }

    private static void hookTextInput(EditText editText) {
        if (!sConfig.isEnableTextInputs()) {
            return;
        }

        // Track text changes
        editText.addTextChangedListener(new android.text.TextWatcher() {
            private int previousLength = editText.getText() != null ? editText.getText().length() : 0;

            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {
                // Not needed for analytics
            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                // Not needed for analytics
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                final int currentLength = editable != null ? editable.length() : 0;
                if (currentLength != previousLength) {
                    final int delta = currentLength - previousLength;
                    final String action = delta > 0 ? "add" : "remove";

                    // Use 'edit' event kind for text input changes
                    final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                        new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(editText)),
                        new AbstractMap.SimpleEntry<>(FieldKey.ACTION, action),
                        new AbstractMap.SimpleEntry<>(FieldKey.VALUE, delta)
                    );
                    emit(EventKind.edit, editText.getClass().getSimpleName(), meta);

                    previousLength = currentLength;
                }
            }
        });

        // Track focus changes while preserving existing listener
        final View.OnFocusChangeListener originalFocusListener = getOnFocusChangeListener(editText);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (originalFocusListener != null) {
                    originalFocusListener.onFocusChange(view, hasFocus);
                }
                final String action = hasFocus ? "focus" : "blur";

                // Use 'edit' event kind for text input focus changes
                final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                    new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(editText)),
                    new AbstractMap.SimpleEntry<>(FieldKey.ACTION, action)
                );
                emit(EventKind.edit, editText.getClass().getSimpleName(), meta);
            }
        });

        // Track text input submission
        final TextView.OnEditorActionListener originalEditorActionListener =
                getOnEditorActionListener(editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId,
                                          android.view.KeyEvent event) {
                final boolean handled = originalEditorActionListener != null
                        && originalEditorActionListener.onEditorAction(textView, actionId, event);
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                        || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO
                        || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                        || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                        || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {

                    // Use 'edit' event kind for text input submission
                    final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                        new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(editText)),
                        new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "submit"),
                        new AbstractMap.SimpleEntry<>(FieldKey.VALUE,
                            editText.getText() != null ? editText.getText().length() : 0)
                    );
                    emit(EventKind.edit, editText.getClass().getSimpleName(), meta);
                }
                return handled;
            }
        });
    }

    private static void hookScrollView(ScrollView scrollView) {
        scrollView.getViewTreeObserver().addOnScrollChangedListener(
            new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    if (sConfig.isEnableScrolls()) {
                        final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                            new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(scrollView)),
                            new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "scroll")
                        );
                        emit(EventKind.drag, scrollView.getClass().getSimpleName(), meta);
                    }
                }
            });
    }

    private static void hookHScrollView(HorizontalScrollView scrollView) {
        scrollView.getViewTreeObserver().addOnScrollChangedListener(
            new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    if (sConfig.isEnableScrolls()) {
                        final Map<String, Object> meta = MetaMapHelper.createMetaMap(
                            new AbstractMap.SimpleEntry<>(FieldKey.ID, viewIdName(scrollView)),
                            new AbstractMap.SimpleEntry<>(FieldKey.ACTION, "scroll")
                        );
                        emit(EventKind.drag, scrollView.getClass().getSimpleName(), meta);
                    }
                }
            });
    }

    // ------- Reflection helpers to preserve existing listeners -----------------------------

    private static View.OnClickListener getOnClickListener(View view) {
        try {
            // Try to get the listener through reflection, but handle failures gracefully
            final java.lang.reflect.Field infoField = View.class.getDeclaredField("mListenerInfo");
            infoField.setAccessible(true);
            final Object info = infoField.get(view);
            if (info == null) {
                return null;
            }

            // Use a more robust approach to get the listener info class
            Class<?> listenerInfoClass;
            try {
                listenerInfoClass = Class.forName("android.view.View$ListenerInfo");
            } catch (ClassNotFoundException e) {
                // Fallback: try to get the class from the info object
                listenerInfoClass = info.getClass();
            }

            final java.lang.reflect.Field field =
                    listenerInfoClass.getDeclaredField("mOnClickListener");
            field.setAccessible(true);
            return (View.OnClickListener) field.get(info);
        } catch (Throwable e) {
            // If reflection fails, we can't preserve the original listener
            // This is acceptable as the library will still work,
            // just without preserving existing listeners
            return null;
        }
    }

    private static CompoundButton.OnCheckedChangeListener getOnCheckedChangeListener(
            CompoundButton view) {
        try {
            final java.lang.reflect.Field field =
                    CompoundButton.class.getDeclaredField("mOnCheckedChangeListener");
            field.setAccessible(true);
            return (CompoundButton.OnCheckedChangeListener) field.get(view);
        } catch (Throwable e) {
            return null;
        }
    }

    private static SeekBar.OnSeekBarChangeListener getOnSeekBarChangeListener(SeekBar view) {
        try {
            final java.lang.reflect.Field field =
                    SeekBar.class.getDeclaredField("mOnSeekBarChangeListener");
            field.setAccessible(true);
            return (SeekBar.OnSeekBarChangeListener) field.get(view);
        } catch (Throwable e) {
            return null;
        }
    }

    private static SearchView.OnQueryTextListener getOnQueryTextListener(SearchView view) {
        try {
            final java.lang.reflect.Field field =
                    SearchView.class.getDeclaredField("mOnQueryChangeListener");
            field.setAccessible(true);
            return (SearchView.OnQueryTextListener) field.get(view);
        } catch (Throwable e) {
            return null;
        }
    }

    private static View.OnFocusChangeListener getOnFocusChangeListener(View view) {
        try {
            final java.lang.reflect.Field field =
                    View.class.getDeclaredField("mOnFocusChangeListener");
            field.setAccessible(true);
            return (View.OnFocusChangeListener) field.get(view);
        } catch (Throwable e) {
            return null;
        }
    }

    private static TextView.OnEditorActionListener getOnEditorActionListener(EditText view) {
        try {
            final java.lang.reflect.Field field =
                    EditText.class.getDeclaredField("mEditorActionListener");
            field.setAccessible(true);
            return (TextView.OnEditorActionListener) field.get(view);
        } catch (Throwable e) {
            return null;
        }
    }
}

