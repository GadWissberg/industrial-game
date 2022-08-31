package com.gadarts.industrial.systems.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ToolTipHandler implements Disposable {
	public static final Color TOOL_TIP_FONT_COLOR = Color.WHITE;
	public static final Color TOOL_TIP_BACKGROUND_COLOR = Color.FOREST;
	private static final float TOOLTIP_PADDING = 4f;
	private static final long TOOLTIP_DELAY = 1000;
	private final GameStage stage;
	@Setter
	private long lastHighlightNodeChange;
	private Texture toolTipBackgroundColor;
	private Table tooltipTable;
	private GlyphLayout toolTipLayout;
	private BitmapFont toolTipFont;

	private String checkIfToolTipIsPickupOrObstacle(final MapGraphNode cursorNode, final MapGraph map) {
		Entity pickup = map.getPickupFromNode(cursorNode);
		if (pickup != null) {
			return ComponentsMapper.pickup.get(pickup).getItem().getDefinition().getDisplayName();
		} else {
			Entity obstacle = map.findObstacleByNode(cursorNode);
			return obstacle != null ? ComponentsMapper.environmentObject.get(obstacle).getType().getDisplayName() : null;
		}
	}

	void handleToolTip(final MapGraph map, final MapGraphNode cursorNode) {
		if (lastHighlightNodeChange != -1 && TimeUtils.timeSinceMillis(lastHighlightNodeChange) >= TOOLTIP_DELAY) {
			String text = calculateToolTipText(map, cursorNode);
			displayToolTip(text);
			lastHighlightNodeChange = -1;
		}
	}

	private String calculateToolTipText(final MapGraph map,
										final MapGraphNode cursorNode) {
		Entity enemyAtNode = map.fetchAliveEnemyFromNode(cursorNode);
		String result;
		if (enemyAtNode != null) {
			result = ComponentsMapper.enemy.get(enemyAtNode).getEnemyDefinition().getDisplayName();
		} else {
			result = checkIfToolTipIsPickupOrObstacle(cursorNode, map);
		}
		return result;
	}

	void displayToolTip(final String text) {
		if (text != null) {
			((Label) tooltipTable.getChild(0)).setText(text);
			tooltipTable.setVisible(true);
			tooltipTable.setPosition(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
			resizeToolTip(text);
		} else {
			tooltipTable.setVisible(false);
		}
	}

	private void resizeToolTip(final CharSequence text) {
		toolTipLayout.setText(toolTipFont, text);
		float width = toolTipLayout.width + TOOLTIP_PADDING * 2;
		float height = toolTipLayout.height + TOOLTIP_PADDING * 2;
		tooltipTable.setSize(width, height);
	}

	void addToolTipTable( ) {
		tooltipTable = new Table();
		setToolTipBackground();
		stage.addActor(tooltipTable);
		toolTipFont = new BitmapFont();
		toolTipLayout = new GlyphLayout();
		resizeToolTip("");
		tooltipTable.add(new Label(null, new Label.LabelStyle(toolTipFont, TOOL_TIP_FONT_COLOR))).row();
		tooltipTable.setVisible(false);
	}

	private void setToolTipBackground( ) {
		Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGB565);
		bgPixmap.setColor(TOOL_TIP_BACKGROUND_COLOR);
		bgPixmap.fill();
		toolTipBackgroundColor = new Texture(bgPixmap);
		bgPixmap.dispose();
		tooltipTable.setBackground(new TextureRegionDrawable(new TextureRegion(toolTipBackgroundColor)));
	}

	@Override
	public void dispose( ) {
		toolTipBackgroundColor.dispose();
		toolTipFont.dispose();
	}

	public void onMouseEnteredNewNode( ) {
		displayToolTip(null);
		setLastHighlightNodeChange(TimeUtils.millis());
	}
}
