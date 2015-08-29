package com.zhsan.gamecomponents.maplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.zhsan.common.Pair;
import com.zhsan.common.Paths;
import com.zhsan.common.Point;
import com.zhsan.gamecomponents.maplayer.troopanimator.TranslateAnimator;
import com.zhsan.gameobject.*;
import com.zhsan.screen.GameScreen;

import java.io.File;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Peter on 8/8/2015.
 */
public class TroopAnimationLayer implements MapLayer {

    public enum PendingTroopAnimationType {
        MOVE, ATTACK
    }

    public static class PendingTroopAnimation {
        public final Troop troop;
        public final PendingTroopAnimationType type;
        public final Point from, to;

        public PendingTroopAnimation(Troop troop, PendingTroopAnimationType type, Point from, Point to) {
            this.troop = troop;
            this.type = type;
            this.from = from;
            this.to = to;
        }
    }

    public static final String TROOP_RES_PATH = Paths.RESOURCES + "Troop" + File.separator;

    private Map<Pair<MilitaryKind, TroopAnimation>, Texture> troopImages = new HashMap<>();

    private int drawFrame = 0;

    private BlockingQueue<PendingTroopAnimation> pendingTroopAnimations = new ArrayBlockingQueue<>(100);

    private List<TranslateAnimator> runningAnimators = new ArrayList<>();
    private Set<Troop> runningTroops = new HashSet<>();

    private Map<Troop, TroopTitleWidget> troopTitleWidgets = new HashMap<>();

    public void addPendingTroopAnimation(TroopAnimationLayer.PendingTroopAnimation animation) {
        pendingTroopAnimations.add(animation);
    }

    public boolean isNoPendingTroopAnimations() {
        return pendingTroopAnimations.size() == 0;
    }

    private TextureRegion getTroopImage(String resSet, Troop t, GameScenario scen) {
        MilitaryKind kind = t.getMilitary().getKind();
        TroopAnimation animation = scen.getTroopAnimations().get(TroopAnimation.TroopAnimationKind.IDLE.getId());

        Pair<MilitaryKind, TroopAnimation> pair = new Pair<>(kind, animation);

        if (!troopImages.containsKey(pair)) {
            FileHandle f = Gdx.files.external(TROOP_RES_PATH + resSet + File.separator + kind.getId() + File.separator + animation.getFileName());
            Texture texture = new Texture(f);
            troopImages.put(pair, texture);
        }

        int frameIndex = drawFrame / animation.getIdleFrame() % animation.getFrameCount();
        int frameDirection = 0;
        int spriteSize = animation.getSpriteSize();
        return new TextureRegion(troopImages.get(pair),
                frameIndex * spriteSize, frameDirection * spriteSize, spriteSize, spriteSize);
    }

    @Override
    public void draw(GameScreen screen, String resPack, DrawingHelpers helpers,
                     int zoom, Batch batch, float parentAlpha) {
        for (PendingTroopAnimation animation : new ArrayList<>(pendingTroopAnimations)) {
            if (runningTroops.contains(animation.troop)) continue;

            runningTroops.add(animation.troop);
            pendingTroopAnimations.remove(animation);
            if (helpers.isMapLocationOnScreen(animation.from) || helpers.isMapLocationOnScreen(animation.to)) {
                runningAnimators.add(new TranslateAnimator(helpers, animation));
            }
        }

        Map<Troop, Point> drawnTroops = new HashMap<>();

        Set<Troop> toDraw = new HashSet<>(screen.getScenario().getTroops().getAll());
        Iterator<TranslateAnimator> animatorIterator = runningAnimators.iterator();
        while (animatorIterator.hasNext()) {
            TranslateAnimator animator = animatorIterator.next();

            Troop t = animator.getAnimation().troop;
            toDraw.remove(t);

            TextureRegion image = getTroopImage(resPack, t, screen.getScenario());
            Point px = animator.step();
            batch.draw(image, px.x, px.y, zoom, zoom);

            drawnTroops.put(t, px);

            if (animator.isCompleted()) {
                animatorIterator.remove();
                runningTroops.remove(t);
            }
        }

        for (Troop t : toDraw) {
            if (helpers.isMapLocationOnScreen(t.getLocation())) {
                TextureRegion image = getTroopImage(resPack, t, screen.getScenario());
                Point px = helpers.getPixelFromMapLocation(t.getLocation());
                batch.draw(image, px.x, px.y, zoom, zoom);

                drawnTroops.put(t, px);
            }
        }

        drawnTroops.entrySet().forEach(t -> {
            TroopTitleWidget widget = troopTitleWidgets.get(t.getKey());
            if (widget == null) {
                widget = new TroopTitleWidget(t.getKey());
                troopTitleWidgets.put(t.getKey(), widget);
            }
            widget.setPosition(t.getValue().x, t.getValue().y + zoom);
            widget.draw(batch, parentAlpha);
        });

        drawFrame++;
    }

    @Override
    public void dispose() {
        troopImages.values().forEach(Texture::dispose);
        troopTitleWidgets.values().forEach(TroopTitleWidget::dispose);
        TroopTitleWidget.disposeAll();
    }

}
