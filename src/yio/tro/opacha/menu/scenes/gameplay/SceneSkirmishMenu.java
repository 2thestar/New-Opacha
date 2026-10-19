package yio.tro.opacha.menu.scenes.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.PlayType;
import yio.tro.opacha.game.loading.LoadingParameters;
import yio.tro.opacha.game.loading.LoadingType;
import yio.tro.opacha.menu.LanguagesManager;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.elements.slider.SliderBehavior;
import yio.tro.opacha.menu.elements.slider.SliderYio;
import yio.tro.opacha.menu.scenes.SceneYio;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.GraphicsYio;

public class SceneSkirmishMenu extends SceneYio {


    public ButtonYio startButton;
    private ButtonYio label;
    private SliderYio levelSizeSlider;
    private SliderYio difficultySlider;
    private ButtonYio innerParent;
    private int colorValues[];
    private SliderYio colorsSlider;
    private CheckButtonYio chkAiOnly;
    private SliderYio opponentsSlider;
    public ButtonYio playTypeButton, routesButton;
    private int playTypeIndex;
    private boolean directional = true;


    @Override
    protected void initialize() {
        setBackground(GroundIndex.YELLOW);

        initColorsValues();
        createBackButton();
        uiFactory.getLabelElement().setSize(.72, .075).centerHorizontal().alignBottom(.875)
                .setFont(yio.tro.opacha.Fonts.titleFont).setString(languagesManager.getString("skirmish"));
        createStartButton();
        createInnerParent();
        createLabel();
        createSliders();
        createTacticsOptions();
        createChecks();

        loadValues();
    }


    private void initColorsValues() {
        colorValues = new int[]{2, 3, 4};
    }


    private void createBackButton() {
        spawnBackButton(new Reaction() {
            @Override
            protected void reaction() {
                saveValues();
                Scenes.chooseGameMode.create();
            }
        });
    }


    private void createInnerParent() {
        innerParent = uiFactory.getButton()
                .setSize(1, 1)
                .centerHorizontal()
                .alignBottom()
                .setAnimation(AnimationYio.none)
                .setRenderable(false)
                .setTouchable(false);
    }


    private void createLabel() {
        label = uiFactory.getButton()
                .setParent(innerParent)
                .setSize(0.86, 0.70)
                .centerHorizontal()
                .alignBottom(.13)
                .applyText(" ")
                .setBackgroundEnabled(false).setBorder(false)
                .setTouchable(false)
                .setAnimation(AnimationYio.none);
    }


    private void createSliders() {
        levelSizeSlider = uiFactory.getSlider()
                .setParent(label)
                .setSize(0.76)
                .alignTop(0.01)
                .setAnimation(AnimationYio.none)
                .centerHorizontal()
                .setName("level_size")
                .setValues(0, 5)
                .setSegmentsVisible(true)
                .setBehavior(new SliderBehavior() {
                    @Override
                    public String getValueString(LanguagesManager languagesManager, SliderYio sliderYio) {
                        return "" + convertSliderIndexIntoLevelSize(sliderYio);
                    }
                });

        difficultySlider = uiFactory.getSlider()
                .clone(previousElement)
                .alignBottom(previousElement, 0.015)
                .setName("difficulty")
                .setAnimation(AnimationYio.none)
                .setValues(0.25, 3)
                .setBehavior(new SliderBehavior() {
                    @Override
                    public String getValueString(LanguagesManager languagesManager, SliderYio sliderYio) {
                        return convertSliderIndexIntoDifficultyString(languagesManager, sliderYio);
                    }
                })
                .setVerticalTouchOffset(0.1f * GraphicsYio.width);

        colorsSlider = uiFactory.getSlider()
                .clone(previousElement)
                .alignBottom(previousElement, 0.015)
                .setName("colors")
                .setAnimation(AnimationYio.none)
                .setValues(0, colorValues.length)
                .setBehavior(new SliderBehavior() {
                    @Override
                    public String getValueString(LanguagesManager languagesManager, SliderYio sliderYio) {
                        return "" + colorValues[sliderYio.getValueIndex()];
                    }
                })
                .setVerticalTouchOffset(0.1f * GraphicsYio.width);

        opponentsSlider = uiFactory.getSlider()
                .clone(previousElement)
                .alignBottom(previousElement, 0.015)
                .setName("ai_opponents")
                .setAnimation(AnimationYio.none)
                .setValues(0, 4)
                .setSegmentsVisible(true)
                .setBehavior(new SliderBehavior() {
                    @Override
                    public String getValueString(LanguagesManager manager, SliderYio slider) {
                        String[] keys = {"ai_mixed", "ai_simple", "ai_savvy", "ai_silly"};
                        return manager.getString(keys[slider.getValueIndex()]);
                    }
                })
                .setVerticalTouchOffset(0.1f * GraphicsYio.width);

    }


    private String convertSliderIndexIntoLevelSize(SliderYio sliderYio) {
        switch (sliderYio.getValueIndex()) {
            default:
            case 0:
                return languagesManager.getString("tiny");
            case 1:
                return languagesManager.getString("small");
            case 2:
                return languagesManager.getString("normal");
            case 3:
                return languagesManager.getString("big");
            case 4:
                return languagesManager.getString("giant");
        }
    }


    private String convertSliderIndexIntoDifficultyString(LanguagesManager languagesManager, SliderYio sliderYio) {
        switch (sliderYio.getValueIndex()) {
            default:
            case 0:
                return languagesManager.getString("easy");
            case 1:
                return languagesManager.getString("normal");
            case 2:
                return languagesManager.getString("hard");
            case 3:
                return languagesManager.getString("impossible");
        }
    }


    private void createChecks() {
        chkAiOnly = uiFactory.getCheckButton()
                .setParent(label)
                .setName("ai_only")
                .alignBottom(0.01)
                .setWidth(0.75)
                .centerHorizontal();
    }

    private void createTacticsOptions() {
        playTypeButton = uiFactory.getButton().setParent(label).setSize(.7, .055)
                .centerHorizontal().alignBottom(.155).setAnimation(AnimationYio.none)
                .setBackgroundEnabled(true).setBorder(true)
                .setReaction(new Reaction() { @Override protected void reaction() {
                    playTypeIndex = (playTypeIndex + 1) % PlayType.values().length; updateTacticsLabels();
                }});
        routesButton = uiFactory.getButton().setParent(label).setSize(.7, .055)
                .centerHorizontal().alignBottom(.085).setAnimation(AnimationYio.none)
                .setBackgroundEnabled(true).setBorder(true)
                .setReaction(new Reaction() { @Override protected void reaction() {
                    directional = !directional; updateTacticsLabels();
                }});
    }

    private void updateTacticsLabels() {
        playTypeButton.applyText("play_" + PlayType.values()[playTypeIndex]);
        routesButton.applyText(directional ? "routes_mixed" : "routes_normal");
    }


    private void loadValues() {
        Preferences prefs = getPreferences();

        levelSizeSlider.setValueIndex(prefs.getInteger("level_size", 1));
        difficultySlider.setValueIndex(prefs.getInteger("difficulty", 1));
        colorsSlider.setValueIndex(prefs.getInteger("colors", 0));
        chkAiOnly.setChecked(prefs.getBoolean("ai_only", false));
        opponentsSlider.setValueIndex(Math.max(0, Math.min(3, prefs.getInteger("opponents", 0))));
        playTypeIndex = Math.max(0, Math.min(3, prefs.getInteger("play_type", 0)));
        directional = prefs.getBoolean("directional", true);
        updateTacticsLabels();
    }


    private void saveValues() {
        Preferences prefs = getPreferences();

        prefs.putInteger("level_size", levelSizeSlider.getValueIndex());
        prefs.putInteger("difficulty", difficultySlider.getValueIndex());
        prefs.putInteger("colors", colorsSlider.getValueIndex());
        prefs.putBoolean("ai_only", chkAiOnly.isChecked());
        prefs.putInteger("opponents", opponentsSlider.getValueIndex());
        prefs.putInteger("play_type", playTypeIndex);
        prefs.putBoolean("directional", directional);

        prefs.flush();
    }


    private Preferences getPreferences() {
        return Gdx.app.getPreferences("vitamin.soccer_parameters");
    }


    private void createStartButton() {
        startButton = uiFactory.getButton()
                .setSize(.82, .067).centerHorizontal().alignBottom(.04)
                .applyText("ui_start_match").setGroundIndex(GroundIndex.YELLOW)
                .setReaction(new Reaction() {
                    @Override
                    protected void reaction() {
                        saveValues();
                        LoadingParameters loadingParameters = new LoadingParameters();
                        loadingParameters.addParameter("map_size", levelSizeSlider.getValueIndex());
                        loadingParameters.addParameter("colors", colorValues[colorsSlider.getValueIndex()]);
                        loadingParameters.addParameter("ai_only", chkAiOnly.isChecked());
                        loadingParameters.addParameter("difficulty", difficultySlider.getValueIndex());
                        loadingParameters.addParameter("opponents", opponentsSlider.getValueIndex());
                        loadingParameters.addParameter("play_type", PlayType.values()[playTypeIndex].toString());
                        loadingParameters.addParameter("directional", directional);
                        loadingParameters.addParameter("seed", YioGdxGame.random.nextInt());
                        yioGdxGame.loadingManager.startInstantly(LoadingType.skirmish_create, loadingParameters);
                    }
                });
    }

}
