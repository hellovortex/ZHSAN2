package com.zhsan.gamecomponents.gameframe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Disposable;
import com.zhsan.common.Paths;
import com.zhsan.common.exception.FileReadException;
import com.zhsan.gamecomponents.common.StateTexture;
import com.zhsan.gamecomponents.common.textwidget.TextWidget;
import com.zhsan.gamecomponents.common.XmlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Created by Peter on 7/3/2015.
 */
public class GameFrame extends WidgetGroup {

    public static final String RES_PATH = Paths.RESOURCES + "GameFrame" + File.separator;

    public interface OnClick {
        public void onOkClicked();
        public void onCancelClicked();
    }

    private static class Edge {
        public final int width;
        public final Texture image;

        private Edge(Node node) {
            width = Integer.parseInt(node.getAttributes().getNamedItem("Width").getNodeValue());

            FileHandle f = Gdx.files.external(RES_PATH + File.separator + "Data" + File.separator +
                    XmlHelper.loadAttribute(node, "FileName"));
            image = new Texture(f);
        }

        void dispose() {
            image.dispose();
        }
    }

    private OnClick buttonListener;

    private Edge leftEdge, rightEdge, topEdge, bottomEdge;
    private Texture background;
    private TextWidget titleWidget;
    private int okWidth, okHeight, cancelWidth, cancelHeight, okRight, okBottom, cancelRight, cancelBottom;
    private Rectangle ok, cancel;
    private StateTexture okTexture, cancelTexture;
    private Sound okSound, cancelSound;
    private Texture topLeft, topRight, bottomLeft, bottomRight;
    private boolean okEnabled, cancelEnabled;

    private void loadXml() {
        FileHandle f = Gdx.files.external(RES_PATH + "GameFrameData.xml");
        String dataPath = RES_PATH + File.separator + "Data" + File.separator;

        Document dom;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.parse(f.read());

            leftEdge = new Edge(dom.getElementsByTagName("LeftEdge").item(0));
            rightEdge = new Edge(dom.getElementsByTagName("RightEdge").item(0));
            topEdge = new Edge(dom.getElementsByTagName("TopEdge").item(0));
            bottomEdge = new Edge(dom.getElementsByTagName("BottomEdge").item(0));

            FileHandle fh;
            fh = Gdx.files.external(dataPath +
                    XmlHelper.loadAttribute(dom.getElementsByTagName("BackGround").item(0), "FileName"));
            background = new Texture(fh);

            titleWidget = new TextWidget(TextWidget.Setting.fromXml(dom.getElementsByTagName("Title").item(0)));
            addActor(titleWidget);

            Node okNode = dom.getElementsByTagName("OKButton").item(0);
            Node cancelNode = dom.getElementsByTagName("CancelButton").item(0);
            okWidth = Integer.parseInt(XmlHelper.loadAttribute(okNode, "Width"));
            okHeight = Integer.parseInt(XmlHelper.loadAttribute(okNode, "Height"));
            okRight = Integer.parseInt(XmlHelper.loadAttribute(okNode, "MarginRight"));
            okBottom = Integer.parseInt(XmlHelper.loadAttribute(okNode, "MarginBottom"));
            cancelWidth = Integer.parseInt(XmlHelper.loadAttribute(cancelNode, "Width"));
            cancelHeight = Integer.parseInt(XmlHelper.loadAttribute(cancelNode, "Height"));
            cancelRight = Integer.parseInt(XmlHelper.loadAttribute(cancelNode, "MarginRight"));
            cancelBottom = Integer.parseInt(XmlHelper.loadAttribute(cancelNode, "MarginBottom"));

            okTexture = StateTexture.fromXml(dataPath, okNode);
            cancelTexture = StateTexture.fromXml(dataPath, cancelNode);

            Node sound = dom.getElementsByTagName("SoundFile").item(0);
            fh = Gdx.files.external(dataPath + XmlHelper.loadAttribute(sound, "OK"));
            okSound = Gdx.audio.newSound(fh);

            fh = Gdx.files.external(dataPath + XmlHelper.loadAttribute(sound, "Cancel"));
            cancelSound = Gdx.audio.newSound(fh);

            fh = Gdx.files.external(dataPath +
                    XmlHelper.loadAttribute(dom.getElementsByTagName("TopLeft").item(0), "FileName"));
            topLeft = new Texture(fh);

            fh = Gdx.files.external(dataPath +
                    XmlHelper.loadAttribute(dom.getElementsByTagName("TopRight").item(0), "FileName"));
            topRight = new Texture(fh);

            fh = Gdx.files.external(dataPath +
                    XmlHelper.loadAttribute(dom.getElementsByTagName("BottomLeft").item(0), "FileName"));
            bottomLeft = new Texture(fh);

            fh = Gdx.files.external(dataPath +
                    XmlHelper.loadAttribute(dom.getElementsByTagName("BottomRight").item(0), "FileName"));
            bottomRight = new Texture(fh);
        } catch (Exception e) {
            throw new FileReadException(RES_PATH + "GameFrameData.xml", e);
        }
    }

    public GameFrame(String title, @Nullable OnClick buttonListener) {
        loadXml();

        this.setWidth(Gdx.graphics.getWidth());
        this.setHeight(Gdx.graphics.getHeight());

        this.setOkEnabled(true);
        this.setCancelEnabled(true);

        titleWidget.setText(title);
        titleWidget.setX(getLeftBound());
        titleWidget.setY(getTopBound());
        titleWidget.setWidth(getRightBound() - getLeftBound());
        titleWidget.setHeight(getHeight() - getTopBound());

        if (buttonListener != null) {
            this.addOnClickListener(buttonListener);
        }
    }

    public void resize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);

        titleWidget.setX(getLeftBound());
        titleWidget.setY(getTopBound());
        titleWidget.setWidth(getRightBound() - getLeftBound());
        titleWidget.setHeight(getHeight() - getTopBound());
    }

    protected final void setTitle(String title) {
        titleWidget.setText(title);
    }

    public final void addOnClickListener(@NotNull OnClick buttonListener) {
        this.buttonListener = buttonListener;
        this.addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                handleMouseMove(x, y);
                event.stop();
                return false;
            }

            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                handleTouchDown(button, x, y);
                event.stop();
                return false;
            }
        });
    }

    @Override
    public final void setWidth(float width) {
        super.setWidth(width);
    }

    @Override
    public final void setHeight(float height) {
        super.setHeight(height);
    }

    protected final float getTopBound() {
        return getHeight() - topEdge.width;
    }

    protected final float getBottomBound() {
        return bottomEdge.width;
    }

    protected final float getBottomActiveBound() {
        return getBottomBound() + okHeight;
    }

    protected final float getLeftBound() {
        return leftEdge.width;
    }

    protected final float getRightBound() {
        return getWidth() - rightEdge.width;
    }

    protected final boolean isOkEnabled() {
        return okEnabled;
    }

    protected final void setOkEnabled(boolean okEnabled) {
        this.okEnabled = okEnabled;
        okTexture.setState(okEnabled ? StateTexture.State.NORMAL : StateTexture.State.DISABLED);
    }

    protected final boolean isCancelEnabled() {
        return cancelEnabled;
    }

    protected final void setCancelEnabled(boolean cancelEnabled) {
        this.cancelEnabled = cancelEnabled;
        cancelTexture.setState(cancelEnabled ? StateTexture.State.NORMAL : StateTexture.State.DISABLED);
    }

    public void draw(Batch batch, float parentAlpha) {
        // edges
        float top = getTopBound();
        float bottom = getBottomBound();
        float left = getLeftBound();
        float right = getRightBound();

        batch.draw(topEdge.image, left, top, right - left, getHeight() - top);
        batch.draw(bottomEdge.image, left, 0, right - left, bottom);
        batch.draw(leftEdge.image, 0, bottom, left, top - bottom);
        batch.draw(rightEdge.image, right, bottom, getWidth() - right, top - bottom);

        // corners
        batch.draw(topLeft, 0, top, left, getHeight() - top);
        batch.draw(topRight, right, top, getWidth() - right, getHeight() - top);
        batch.draw(bottomLeft, 0, 0, left, bottom);
        batch.draw(bottomRight, right, 0, getWidth() - right, bottom);

        // background
        batch.draw(background, left, bottom, right - left, top - bottom);

        // ok button
        if (ok == null) {
            ok = new Rectangle();
        }
        ok.setWidth(okWidth);
        ok.setHeight(okHeight);
        ok.setX(getWidth() - okRight - okWidth);
        ok.setY(okBottom);
        batch.draw(okTexture.get(), ok.x, ok.y, ok.width, ok.height);

        // cancel button
        if (cancel == null) {
            cancel = new Rectangle();
        }
        cancel.setWidth(cancelWidth);
        cancel.setHeight(cancelHeight);
        cancel.setX(getWidth() - cancelRight - cancelWidth);
        cancel.setY(cancelBottom);
        batch.draw(cancelTexture.get(), cancel.x, cancel.y, cancel.width, cancel.height);

        // content
        super.draw(batch, parentAlpha);
    }

    private void handleMouseMove(float x, float y) {
        if (ok != null && ok.contains(x, y) && isOkEnabled()) {
            okTexture.setState(StateTexture.State.SELECTED);
        } else {
            okTexture.setState(isOkEnabled() ? StateTexture.State.NORMAL : StateTexture.State.DISABLED);
        }
        if (cancel != null && cancel.contains(x, y) && isCancelEnabled()) {
            cancelTexture.setState(StateTexture.State.SELECTED);
        } else {
            cancelTexture.setState(isCancelEnabled() ? StateTexture.State.NORMAL : StateTexture.State.DISABLED);
        }
    }

    private void handleTouchDown(int button, float x, float y) {
        if (ok != null && ok.contains(x, y) && isOkEnabled()) {
            okTexture.setState(StateTexture.State.NORMAL);
            buttonListener.onOkClicked();
            dismiss(true);
        }
        if (cancel != null && ((cancel.contains(x, y) || button == Input.Buttons.RIGHT) && isCancelEnabled())) {
            buttonListener.onCancelClicked();
            dismiss(false);
        }
    }

    protected final void dismiss(boolean ok) {
        if (ok) {
            okSound.play();
        } else {
            cancelSound.play();
        }
        this.setVisible(false);
    }

    public void dispose() {
        disposeAllChildren(this);
        leftEdge.dispose();
        rightEdge.dispose();
        topEdge.dispose();
        bottomEdge.dispose();
        background.dispose();
        okTexture.dispose();
        cancelTexture.dispose();
        okSound.dispose();
        cancelSound.dispose();
        topLeft.dispose();
        topRight.dispose();
        bottomLeft.dispose();
        bottomRight.dispose();
    }

    private void disposeAllChildren(WidgetGroup root) {
        for (Actor i : root.getChildren()) {
            if (i instanceof Disposable) {
                ((Disposable) i).dispose();
            } else if (i instanceof WidgetGroup) {
                disposeAllChildren((WidgetGroup) i);
            }
        }
    }

}
