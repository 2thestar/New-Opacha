package yio.tro.opacha.game.touch_modes;

import yio.tro.opacha.game.gameplay.EditorWorker;
import yio.tro.opacha.game.gameplay.model.FractionType;
import yio.tro.opacha.game.gameplay.model.Planet;
import yio.tro.opacha.game.gameplay.model.PlanetType;
import yio.tro.opacha.menu.LanguagesManager;
import yio.tro.opacha.menu.elements.AnimationYio;
import yio.tro.opacha.menu.elements.LightBottomPanelElement;
import yio.tro.opacha.menu.elements.slider.SliderBehavior;
import yio.tro.opacha.menu.elements.slider.SliderYio;
import yio.tro.opacha.menu.scenes.ModalSceneYio;
import yio.tro.opacha.stuff.GraphicsYio;

public class SceneEditSinglePlanet extends ModalSceneYio{

    Planet selectedPlanet;
    private SliderYio fractionsSlider;
    private SliderYio typeSlider;
    int unitValues[];
    private SliderYio unitsSlider;
    private LightBottomPanelElement lightBottomPanelElement;
    public yio.tro.opacha.menu.elements.ButtonYio flagButton, aiButton, stockButton, trapsButton;
    private boolean loadingValues;


    @Override
    protected void initialize() {
        initUnitValues();
        createCloseButton();
        createLightBottomPanel();
        createInternals();
    }


    private void initUnitValues() {
        unitValues = new int[]{0, 1, 2, 3, 5, 7, 10, 12, 15, 20, 25, 50, 100, 200, 500, 1000};
    }


    private void createLightBottomPanel() {
        lightBottomPanelElement = uiFactory.getLightBottomPanelElement()
                .setSize(1, 0.77)
                .setTitle("planet")
                .setAnimation(AnimationYio.down);
    }


    private void createInternals() {
        fractionsSlider = uiFactory.getSlider()
                .setParent(lightBottomPanelElement)
                .setSize(0.7)
                .alignTop(0.07)
                .setAnimation(AnimationYio.none)
                .centerHorizontal()
                .setName("fraction")
                .setValues(0, FractionType.values().length)
                .setSegmentsVisible(true)
                .setTitleDownwordsOffset(GraphicsYio.convertToWidth(0.03))
                .setBehavior(getFractionsSliderBehavior());

        typeSlider = uiFactory.getSlider()
                .setParent(lightBottomPanelElement)
                .setSize(0.7)
                .alignBottom(previousElement, 0.03)
                .setAnimation(AnimationYio.none)
                .centerHorizontal()
                .setName("upgrade")
                .setValues(0, PlanetType.values().length)
                .setSegmentsVisible(true)
                .setTitleDownwordsOffset(GraphicsYio.convertToWidth(0.03))
                .setBehavior(getTypeSliderBehavior());

        unitsSlider = uiFactory.getSlider()
                .setParent(lightBottomPanelElement)
                .setSize(0.7)
                .alignBottom(previousElement, 0.03)
                .setAnimation(AnimationYio.none)
                .centerHorizontal()
                .setName("power")
                .setValues(0, unitValues.length)
                .setTitleDownwordsOffset(GraphicsYio.convertToWidth(0.03))
                .setBehavior(getUnitsSliderBehavior());
        aiButton = uiFactory.getButton().setParent(lightBottomPanelElement).setSize(.80,.055)
                .centerHorizontal().alignBottom(.25).setAnimation(AnimationYio.none).applyText("AI")
                .setReaction(new yio.tro.opacha.menu.reactions.Reaction() {
                    @Override protected void reaction() {
                        if (selectedPlanet == null || selectedPlanet.isNeutral()) return;
                        yio.tro.opacha.game.gameplay.ai.AiType current = yio.tro.opacha.game.GameRules.aiTypes.get(selectedPlanet.fraction);
                        int next = current == null ? 1 : (current.ordinal() + 1) % 3;
                        yio.tro.opacha.game.GameRules.aiTypes.put(selectedPlanet.fraction, yio.tro.opacha.game.gameplay.ai.AiType.values()[next]);
                        updateAiLabel();
                    }
                });
        stockButton = uiFactory.getButton().setParent(lightBottomPanelElement).setSize(.80,.055)
                .centerHorizontal().alignBottom(.175).setAnimation(AnimationYio.none).applyText("Bombs")
                .setReaction(new yio.tro.opacha.menu.reactions.Reaction() {
                    @Override protected void reaction() {
                        if (selectedPlanet.isNeutral()) return;
                        selectedPlanet.bombStock = (selectedPlanet.bombStock + 1) % 6; updateAiLabel();
                    }
                });
        trapsButton = uiFactory.getButton().setParent(lightBottomPanelElement).setSize(.80,.055)
                .centerHorizontal().alignBottom(.10).setAnimation(AnimationYio.none).applyText("Node bombs")
                .setReaction(new yio.tro.opacha.menu.reactions.Reaction() {
                    @Override protected void reaction() {
                        if (selectedPlanet.isNeutral()) return;
                        selectedPlanet.nodeBombs = (selectedPlanet.nodeBombs + 1) % 4; updateAiLabel();
                    }
                });
        flagButton = uiFactory.getButton().setParent(lightBottomPanelElement).setSize(.80,.055)
                .centerHorizontal().alignBottom(.025).setAnimation(AnimationYio.none).applyText("editor_set_flag")
                .setReaction(new yio.tro.opacha.menu.reactions.Reaction() {
                    @Override protected void reaction() { yioGdxGame.gameController.objectsLayer.editorWorker.markFlag(selectedPlanet); }
                });
    }

    private void updateAiLabel() {
        yio.tro.opacha.game.gameplay.ai.AiType type = yio.tro.opacha.game.GameRules.aiTypes.get(selectedPlanet.fraction);
        aiButton.applyText(selectedPlanet.isNeutral() ? "editor_no_ai" : "AI: " + (type == null ? "simple" : type.toString()));
        stockButton.applyText(LanguagesManager.getInstance().getString("editor_bombs") + ": "
                + selectedPlanet.bombStock);
        trapsButton.applyText(LanguagesManager.getInstance().getString("editor_node_bombs") + ": " + selectedPlanet.nodeBombs + "/3");
    }


    private SliderBehavior getUnitsSliderBehavior() {
        return new SliderBehavior() {
            @Override
            public String getValueString(LanguagesManager languagesManager, SliderYio sliderYio) {
                return "" + unitValues[sliderYio.getValueIndex()];
            }


            @Override
            public void onValueChanged(SliderYio sliderYio) {
                applyUnitsValue();
            }
        };
    }


    private SliderBehavior getTypeSliderBehavior() {
        return new SliderBehavior() {
            @Override
            public String getValueString(LanguagesManager languagesManager, SliderYio sliderYio) {
                PlanetType planetType = PlanetType.values()[sliderYio.getValueIndex()];
                return LanguagesManager.getInstance().getString("" + planetType);
            }


            @Override
            public void onValueChanged(SliderYio sliderYio) {
                applyTypeValue();
            }
        };
    }


    private SliderBehavior getFractionsSliderBehavior() {
        return new SliderBehavior() {
            @Override
            public String getValueString(LanguagesManager languagesManager, SliderYio sliderYio) {
                FractionType fractionType = FractionType.values()[sliderYio.getValueIndex()];
                String string = LanguagesManager.getInstance().getString("" + fractionType);
                return string.substring(0, 1).toLowerCase() + string.substring(1);
            }


            @Override
            public void onValueChanged(SliderYio sliderYio) {
                applyFractionValue();
            }
        };
    }


    void applyFractionValue() {
        if (loadingValues) return;
        FractionType fractionType = FractionType.values()[fractionsSlider.getValueIndex()];
        if (selectedPlanet.fraction == fractionType) return;

        selectedPlanet.setFraction(fractionType);
        updateAiLabel();
        EditorWorker editorWorker = yioGdxGame.gameController.objectsLayer.editorWorker;
        editorWorker.onPlanetFractionChanged(selectedPlanet);
        applyTypeValue();
        applyMaxShields();
    }


    void applyTypeValue() {
        if (loadingValues) return;
        PlanetType planetType = PlanetType.values()[typeSlider.getValueIndex()];
        if (selectedPlanet.type == planetType) return;

        selectedPlanet.setType(planetType);
        applyMaxShields();
    }


    void applyUnitsValue() {
        if (loadingValues) return;
        selectedPlanet.setUnitsInside(unitValues[unitsSlider.getValueIndex()]);
    }


    void applyMaxShields() {
        if (selectedPlanet.isEconomic()) return;
        selectedPlanet.setCurrentDefense(selectedPlanet.maxDefense);
    }


    void loadValues() {
        loadingValues = true;
        initUnitValues();
        java.util.TreeSet<Integer> choices = new java.util.TreeSet<>();
        for (int value : unitValues) choices.add(value);
        choices.add((int) selectedPlanet.unitsInside);
        unitValues = new int[choices.size()];
        int position = 0; for (int value : choices) unitValues[position++] = value;
        unitsSlider.setValues(0,unitValues.length);
        FractionType fraction = selectedPlanet.fraction;
        fractionsSlider.setValueIndex(fraction.ordinal());

        PlanetType type = selectedPlanet.type;
        typeSlider.setValueIndex(type.ordinal());

        int unitsInside = (int) selectedPlanet.unitsInside;
        for (int i = 0; i < unitValues.length; i++) {
            int unitValue = unitValues[i];
            if (unitsInside > unitValue) continue;
            unitsSlider.setValueIndex(i);
            break;
        }
        updateAiLabel();
        loadingValues = false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (selectedPlanet != null) {
            selectedPlanet.deselect();
        }
    }


    public void setSelectedPlanet(Planet selectedPlanet) {
        this.selectedPlanet = selectedPlanet;
        loadValues();
    }
}
