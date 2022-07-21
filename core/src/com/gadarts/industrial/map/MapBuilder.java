package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.DefaultGameSettings;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.character.CharacterData;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.player.PlayerComponent;
import com.gadarts.industrial.components.sd.RelatedDecal;
import com.gadarts.industrial.components.sd.SimpleDecalComponent;
import com.gadarts.industrial.shared.WallCreator;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.assets.MapJsonKeys;
import com.gadarts.industrial.shared.model.Coords;
import com.gadarts.industrial.shared.model.GeneralUtils;
import com.gadarts.industrial.shared.model.RelativeBillboard;
import com.gadarts.industrial.shared.model.characters.CharacterDefinition;
import com.gadarts.industrial.shared.model.characters.CharacterTypes;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.attributes.Accuracy;
import com.gadarts.industrial.shared.model.characters.attributes.Agility;
import com.gadarts.industrial.shared.model.characters.enemies.Enemies;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.shared.model.env.EnvironmentObjectDefinition;
import com.gadarts.industrial.shared.model.env.EnvironmentObjectType;
import com.gadarts.industrial.shared.model.env.ThingsDefinitions;
import com.gadarts.industrial.shared.model.map.MapNodeData;
import com.gadarts.industrial.shared.model.map.NodeWalls;
import com.gadarts.industrial.shared.model.map.Wall;
import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;
import com.gadarts.industrial.systems.enemy.EnemySystem;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

import static com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP;
import static com.gadarts.industrial.components.ComponentsMapper.character;
import static com.gadarts.industrial.components.ComponentsMapper.modelInstance;
import static com.gadarts.industrial.shared.assets.Assets.*;
import static com.gadarts.industrial.shared.assets.Assets.Atlases.*;
import static com.gadarts.industrial.shared.assets.Assets.SurfaceTextures.BLANK;
import static com.gadarts.industrial.shared.assets.Assets.SurfaceTextures.MISSING;
import static com.gadarts.industrial.shared.assets.Assets.UiTextures.*;
import static com.gadarts.industrial.shared.assets.MapJsonKeys.*;
import static com.gadarts.industrial.shared.model.characters.CharacterTypes.*;
import static com.gadarts.industrial.shared.model.characters.Direction.NORTH;
import static com.gadarts.industrial.shared.model.characters.Direction.SOUTH;
import static com.gadarts.industrial.shared.model.characters.SpriteType.IDLE;
import static com.gadarts.industrial.shared.model.map.MapNodesTypes.OBSTACLE_KEY_DIAGONAL_FORBIDDEN;
import static com.gadarts.industrial.utils.EntityBuilder.beginBuildingEntity;
import static java.lang.String.format;

public class MapBuilder implements Disposable {
	public static final int PLAYER_HEALTH = 64;

	public static final String MAP_PATH_TEMP = "assets/maps/%s.json";
	public static final float INDEPENDENT_LIGHT_RADIUS = 4f;
	public static final float INDEPENDENT_LIGHT_INTENSITY = 1f;
	public static final String BOUNDING_BOX_PREFIX = "box_";
	private static final CharacterSoundData auxCharacterSoundData = new CharacterSoundData();
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector3 auxVector3_3 = new Vector3();
	private static final Vector3 auxVector3_4 = new Vector3();
	private static final Vector3 auxVector3_5 = new Vector3();
	private static final BoundingBox auxBoundingBox = new BoundingBox();
	private static final String KEY_PICKUPS = "pickups";
	private static final String REGION_NAME_BULLET = "bullet";
	private static final Matrix4 auxMatrix = new Matrix4();
	private static final String KEY_LIGHTS = "lights";
	private static final float INDEPENDENT_LIGHT_HEIGHT = 1F;
	private static final String KEY_ENVIRONMENT = "environment";
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private final Model floorModel;
	private final GameAssetsManager assetsManager;
	private final Gson gson = new Gson();
	private final WallCreator wallCreator;
	private final Map<Enemies, Animation<TextureAtlas.AtlasRegion>> enemyBulletsTextureRegions = new HashMap<>();
	private PooledEngine engine;

	public MapBuilder(PooledEngine engine, GameAssetsManager assetsManager) {
		this.engine = engine;
		this.assetsManager = assetsManager;
		this.wallCreator = new WallCreator(assetsManager);
		floorModel = createFloorModel();
	}

	private Model createFloorModel( ) {
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder meshPartBuilder = modelBuilder.part("floor",
				GL20.GL_TRIANGLES,
				Usage.Position | Usage.Normal | Usage.TextureCoordinates,
				createFloorMaterial());
		createRect(meshPartBuilder);
		return modelBuilder.end();
	}

	/**
	 * Creates the test map.
	 *
	 * @return The Inflated map.
	 */
	public MapGraph inflateTestMap(final String map) {
		JsonObject mapJsonObj = gson.fromJson(Gdx.files.internal(format(MAP_PATH_TEMP, map)).reader(), JsonObject.class);
		JsonObject tilesJsonObject = mapJsonObj.get(TILES).getAsJsonObject();
		MapGraph mapGraph = createMapGraph(mapJsonObj);
		inflateNodes(tilesJsonObject, mapGraph);
		inflateHeights(mapJsonObj, mapGraph);
		inflateAllElements(mapJsonObj, mapGraph);
		mapGraph.init();
		return mapGraph;
	}

	private void inflateNodes(JsonObject tilesJsonObject, MapGraph mapGraph) {
		String matrix = tilesJsonObject.get(MATRIX).getAsString();
		byte[] matrixByte = Base64.getDecoder().decode(matrix.getBytes());
		floorModel.calculateBoundingBox(auxBoundingBox);
		int width = mapGraph.getWidth();
		int depth = mapGraph.getDepth();
		IntStream.range(0, depth).forEach(row ->
				IntStream.range(0, width).forEach(col -> {
					byte currentValue = matrixByte[row * width + col % depth];
					if (currentValue != 0) {
						inflateNode(row, col, currentValue, mapGraph.getNode(col, row));
					}
				}));
		linkFloorEntitiesToNodes(mapGraph);
	}

	private void linkFloorEntitiesToNodes(MapGraph mapGraph) {
		ImmutableArray<Entity> floorEntities = engine.getEntitiesFor(Family.all(FloorComponent.class).get());
		floorEntities.forEach(entity -> {
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(entity).getModelInstance();
			Vector3 pos = modelInstance.transform.getTranslation(auxVector3_1);
			mapGraph.getNode(pos).setEntity(entity);
		});
	}

	private MapGraphNode getNodeByJson(final MapGraph mapGraph, final JsonObject tileJsonObject) {
		int row = tileJsonObject.get(ROW).getAsInt();
		int col = tileJsonObject.get(COL).getAsInt();
		return mapGraph.getNode(col, row);
	}

	private void inflateHeights(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		JsonElement heightsElement = mapJsonObject.get(TILES).getAsJsonObject().get(HEIGHTS);
		Optional.ofNullable(heightsElement).ifPresent(element -> {
			JsonArray heights = element.getAsJsonArray();
			heights.forEach(jsonElement -> {
				JsonObject tileJsonObject = jsonElement.getAsJsonObject();
				MapGraphNode node = getNodeByJson(mapGraph, tileJsonObject);
				float height = tileJsonObject.get(HEIGHT).getAsFloat();
				node.setHeight(height);
				Optional.ofNullable(node.getEntity())
						.ifPresent(e -> modelInstance.get(e).getModelInstance().transform.translate(0, height, 0));
			});
			heights.forEach(jsonElement -> {
				JsonObject tileJsonObject = jsonElement.getAsJsonObject();
				MapGraphNode node = getNodeByJson(mapGraph, tileJsonObject);
				float height = tileJsonObject.get(HEIGHT).getAsFloat();
				inflateWalls(tileJsonObject, node, height, mapGraph);
			});
		});
		calculateNodesAmbientOcclusionValue(mapGraph);
	}

	private void calculateNodesAmbientOcclusionValue(MapGraph mapGraph) {
		mapGraph.getNodes().forEach(node -> {
			int nodeAmbientOcclusionValue = 0;
			float height = node.getHeight();
			for (Direction direction : Direction.values()) {
				Vector2 vec = direction.getDirection(auxVector2_1);
				MapGraphNode nearbyNode = mapGraph.getNode((int) (node.getCol() + vec.x), (int) (node.getRow() + vec.y));
				if (nearbyNode != null && nearbyNode.getHeight() > height) {
					nodeAmbientOcclusionValue |= direction.getMask();
				}
			}
			node.setNodeAmbientOcclusionValue(nodeAmbientOcclusionValue);
		});
	}

	private void inflateEastWall(final JsonObject tileJsonObject,
								 final MapGraphNode node,
								 final float height,
								 final MapGraph mapGraph) {
		int col = node.getCol();
		int eastCol = col + 1;
		JsonElement east = tileJsonObject.get(EAST);
		if (eastCol < mapGraph.getWidth()) {
			if (height != mapGraph.getNode(eastCol, node.getRow()).getHeight() && east != null) {
				JsonObject asJsonObject = east.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applyEastWall(node, height, mapGraph, wallParameters, eastCol);
			}
		} else {
			applyEastWall(node, height, mapGraph, new WallParameters(BLANK), eastCol);
		}
	}

	private void applyEastWall(MapGraphNode node,
							   float height,
							   MapGraph mapGraph,
							   WallParameters wallParameters,
							   int eastCol) {
		if (wallParameters.getDefinition() != MISSING) {
			MapNodeData nodeData = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			NodeWalls walls = nodeData.getWalls();
			walls.setEastWall(WallCreator.createEastWall(
					nodeData,
					wallCreator.getWallModel(),
					assetsManager,
					wallParameters.getDefinition()));
			MapNodeData eastNode = new MapNodeData(node.getRow(), eastCol, OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			nodeData.lift(height);
			if (eastCol < mapGraph.getWidth()) {
				eastNode.setHeight(mapGraph.getNode(eastNode.getCoords()).getHeight());
			} else {
				eastNode.setHeight(0);
			}
			WallCreator.adjustWallBetweenEastAndWest(
					eastNode,
					nodeData,
					wallParameters.getVScale(),
					wallParameters.getHOffset(),
					wallParameters.getVOffset());
			boolean westHigherThanEast = node.getHeight() > eastNode.getHeight()
					&& eastNode.getCoords().getCol() < mapGraph.getWidth();
			inflateWall(walls.getEastWall(), westHigherThanEast ? eastNode : nodeData, mapGraph);
		}
	}

	private void inflateNorthWall(final JsonObject tileJsonObject,
								  final MapGraphNode node,
								  final float height,
								  final MapGraph mapGraph) {
		int col = node.getCol();
		int row = node.getRow();
		int northNodeRow = row - 1;
		JsonElement north = tileJsonObject.get(MapJsonKeys.NORTH);
		if (northNodeRow >= 0) {
			if (height != mapGraph.getNode(col, northNodeRow).getHeight() && north != null) {
				JsonObject asJsonObject = north.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applyNorthWall(node, height, mapGraph, northNodeRow, wallParameters);
			}
		} else {
			applyNorthWall(node, height, mapGraph, northNodeRow, new WallParameters(BLANK));
		}
	}

	private void applyNorthWall(MapGraphNode node,
								float height,
								MapGraph mapGraph,
								int northNodeRow,
								WallParameters wallParameters) {
		if (wallParameters.getDefinition() != MISSING) {
			MapNodeData n = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			n.getWalls().setNorthWall(WallCreator.createNorthWall(
					n,
					wallCreator.getWallModel(),
					assetsManager,
					wallParameters.getDefinition()));
			MapNodeData northNode = new MapNodeData(northNodeRow, n.getCoords().getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			n.lift(height);
			if (northNodeRow >= 0) {
				northNode.setHeight(mapGraph.getNode(northNode.getCoords()).getHeight());
			} else {
				northNode.setHeight(0);
			}
			WallCreator.adjustWallBetweenNorthAndSouth(
					n,
					northNode,
					wallParameters.getVScale(),
					wallParameters.getHOffset(),
					wallParameters.getVOffset());
			boolean northHigherThanSouth = node.getHeight() > northNode.getHeight() && northNodeRow >= 0;
			inflateWall(n.getWalls().getNorthWall(), northHigherThanSouth ? northNode : n, mapGraph);
		}
	}

	private void avoidZeroDimensions(final BoundingBox bBox) {
		Vector3 center = bBox.getCenter(auxVector3_1);
		if (bBox.getWidth() == 0) {
			center.x += 0.01;
		}
		if (bBox.getHeight() == 0) {
			center.y += 0.01;
		}
		if (bBox.getDepth() == 0) {
			center.z += 0.01;
		}
		bBox.ext(center);
	}

	private void inflateWall(Wall wall, MapNodeData parentNodeData, MapGraph mapGraph) {
		BoundingBox bBox = wall.getModelInstance().calculateBoundingBox(new BoundingBox());
		avoidZeroDimensions(bBox);
		bBox.mul(auxMatrix.set(wall.getModelInstance().transform).setTranslation(Vector3.Zero));
		GameModelInstance modelInstance = new GameModelInstance(wall.getModelInstance(), bBox, true, Color.WHITE);
		beginBuildingEntity(engine).addModelInstanceComponent(modelInstance, true)
				.addWallComponent(mapGraph.getNode(parentNodeData.getCoords()))
				.finishAndAddToEngine();
	}

	private void inflateWalls(final JsonObject tileJsonObject,
							  final MapGraphNode node,
							  final float height,
							  final MapGraph mapGraph) {
		inflateEastWall(tileJsonObject, node, height, mapGraph);
		inflateSouthWall(tileJsonObject, node, height, mapGraph);
		inflateWestWall(tileJsonObject, node, height, mapGraph);
		inflateNorthWall(tileJsonObject, node, height, mapGraph);
	}

	private void inflateWestWall(final JsonObject tileJsonObject,
								 final MapGraphNode node,
								 final float height,
								 final MapGraph mapGraph) {
		int col = node.getCol();
		int row = node.getRow();
		int westNodeCol = col - 1;
		JsonElement west = tileJsonObject.get(WEST);
		if (westNodeCol >= 0) {
			if (height != mapGraph.getNode(westNodeCol, row).getHeight() && west != null) {
				JsonObject asJsonObject = west.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applyWestWall(node, height, mapGraph, westNodeCol, wallParameters);
			}
		} else {
			applyWestWall(node, height, mapGraph, westNodeCol, new WallParameters(SurfaceTextures.BLANK));
		}
	}

	private WallParameters inflateWallParameters(JsonObject asJsonObject) {
		SurfaceTextures definition = SurfaceTextures.valueOf(asJsonObject.get(TEXTURE).getAsString());
		return new WallParameters(
				asJsonObject.has(V_SCALE) ? asJsonObject.get(V_SCALE).getAsFloat() : 0,
				asJsonObject.has(H_OFFSET) ? asJsonObject.get(H_OFFSET).getAsFloat() : 0,
				asJsonObject.has(V_OFFSET) ? asJsonObject.get(V_OFFSET).getAsFloat() : 0,
				definition);
	}

	private void applyWestWall(MapGraphNode node,
							   float height,
							   MapGraph mapGraph,
							   int westNodeCol,
							   WallParameters wallParameters) {
		SurfaceTextures definition = wallParameters.getDefinition();
		if (definition != MISSING) {
			MapNodeData nodeData = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			NodeWalls walls = nodeData.getWalls();
			walls.setWestWall(WallCreator.createWestWall(
					nodeData,
					wallCreator.getWallModel(),
					assetsManager,
					definition));
			Coords coords = nodeData.getCoords();
			MapNodeData westNodeData = new MapNodeData(
					coords.getRow(),
					westNodeCol,
					OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			nodeData.lift(height);
			if (westNodeCol >= 0 && westNodeCol < mapGraph.getWidth()) {
				westNodeData.setHeight(mapGraph.getNode(westNodeData.getCoords()).getHeight());
			}
			WallCreator.adjustWallBetweenEastAndWest(
					nodeData,
					westNodeData,
					wallParameters.getVScale(),
					wallParameters.getHOffset(),
					wallParameters.getVOffset());
			boolean eastHigherThanWest = node.getHeight() > westNodeData.getHeight()
					&& westNodeCol >= 0;
			inflateWall(walls.getWestWall(), eastHigherThanWest ? westNodeData : nodeData, mapGraph);
		}
	}

	private void inflateSouthWall(final JsonObject tileJsonObject,
								  final MapGraphNode node,
								  final float height,
								  final MapGraph mapGraph) {
		int row = node.getRow();
		int col = node.getCol();
		int southNodeRow = row + 1;
		JsonElement south = tileJsonObject.get(MapJsonKeys.SOUTH);
		if (southNodeRow < mapGraph.getDepth()) {
			if (height != mapGraph.getNode(col, southNodeRow).getHeight() && south != null) {
				JsonObject asJsonObject = south.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applySouthWall(node, height, mapGraph, southNodeRow, wallParameters);
			}
		} else {
			applySouthWall(node, height, mapGraph, southNodeRow, new WallParameters(BLANK));
		}
	}

	private void applySouthWall(MapGraphNode node, float height, MapGraph mapGraph, int southNodeRow, WallParameters wallParameters) {
		if (wallParameters.getDefinition() != MISSING) {
			MapNodeData nodeData = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			NodeWalls walls = nodeData.getWalls();
			walls.setSouthWall(WallCreator.createSouthWall(
					nodeData,
					wallCreator.getWallModel(),
					assetsManager,
					wallParameters.getDefinition()));
			MapNodeData southNode = new MapNodeData(
					southNodeRow,
					nodeData.getCoords().getCol(),
					OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			nodeData.lift(height);
			if (southNodeRow < mapGraph.getDepth()) {
				southNode.setHeight(mapGraph.getNode(southNode.getCoords()).getHeight());
			} else {
				southNode.setHeight(0);
			}
			WallCreator.adjustWallBetweenNorthAndSouth(
					southNode,
					nodeData,
					wallParameters.getVScale(),
					wallParameters.getHOffset(),
					wallParameters.getVOffset());
			boolean northHigherThanSouth = node.getHeight() > southNode.getHeight();
			inflateWall(walls.getSouthWall(), northHigherThanSouth ? southNode : nodeData, mapGraph);
		}
	}

	private void inflateAllElements(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		inflateCharacters(mapJsonObject, mapGraph);
		inflateLights(mapJsonObject, mapGraph);
		inflateEnvironment(mapJsonObject, mapGraph);
		inflatePickups(mapJsonObject, mapGraph);
	}

	private void inflateEnvObstacleComponent(final Coords coord,
											 final EnvironmentObjectDefinition type,
											 final EntityBuilder builder,
											 final Direction facingDirection) {
		int col = coord.getCol();
		int row = coord.getRow();
		int halfWidth = type.getWidth() / 2;
		int halfDepth = type.getDepth() / 2;
		if (facingDirection == NORTH || facingDirection == SOUTH) {
			int swap = halfWidth;
			halfWidth = halfDepth;
			halfDepth = swap;
		}
		Vector2 topLeft = auxVector2_1.set(col - halfWidth, row - halfDepth);
		Vector2 bottomRight = auxVector2_2.set(col + Math.max(halfWidth, 1) - 1, row + Math.max(halfDepth, 1) - 1);
		builder.addObstacleComponent(topLeft, bottomRight, type);
	}

	private GameModelInstance inflateEnvironmentModelInstance(final MapGraphNode node,
															  final int directionIndex,
															  final EnvironmentObjectDefinition type,
															  final float height) {
		Models def = type.getModelDefinition();
		String fileName = BOUNDING_BOX_PREFIX + def.getFilePath();
		BoundingBox box = assetsManager.get(fileName, BoundingBox.class);
		GameModelInstance modelInstance = new GameModelInstance(assetsManager.getModel(def), box, true, def);
		Direction direction = Direction.values()[directionIndex];
		modelInstance.transform.setTranslation(auxVector3_1.set(node.getCol() + 0.5f, 0, node.getRow() + 0.5f));
		modelInstance.transform.rotate(Vector3.Y, -1 * direction.getDirection(auxVector2_1).angleDeg());
		modelInstance.transform.translate(type.getOffset(auxVector3_1));
		if (type instanceof ThingsDefinitions) {
			ThingsDefinitions.handleEvenSize((ThingsDefinitions) type, modelInstance, direction);
		}
		modelInstance.transform.translate(0, node.getHeight() + height, 0);
		return modelInstance;
	}

	private GameModelInstance inflateEnvModelInstanceComponent(final MapGraphNode node,
															   final JsonObject envJsonObj,
															   final EnvironmentObjectDefinition type,
															   final EntityBuilder builder) {
		float height = envJsonObj.get(HEIGHT).getAsFloat();
		GameModelInstance mi = inflateEnvironmentModelInstance(node, envJsonObj.get(DIRECTION).getAsInt(), type, height);
		mi.getAdditionalRenderData().setColorWhenOutside(Color.WHITE);
		GeneralUtils.applyExplicitModelTexture(type.getModelDefinition(), mi, assetsManager);
		builder.addModelInstanceComponent(mi, true);
		Optional.ofNullable(type.getAppendixModelDefinition())
				.ifPresent(a -> {
					GameModelInstance appendixModelInstance = new GameModelInstance(assetsManager.getModel(a));
					appendixModelInstance.transform.set(mi.transform);
					builder.addAppendixModelInstanceComponent(appendixModelInstance);
				});
		return mi;
	}

	private void inflateEnvLightComponent(final EntityBuilder builder,
										  final EnvironmentObjectDefinition type,
										  final GameModelInstance mi,
										  final int dirIndex) {
		Optional.ofNullable(type.getLightEmission()).ifPresent(l -> {
			float degrees = Direction.values()[dirIndex].getDirection(auxVector2_1).angleDeg();
			Vector3 relativePosition = l.getRelativePosition(auxVector3_2).rotate(Vector3.Y, degrees);
			Vector3 position = mi.transform.getTranslation(auxVector3_1).add(relativePosition);
			builder.addStaticLightComponent(position, l.getIntensity(), l.getRadius(), Color.WHITE);
		});
	}

	private void inflateEnvComponents(EnvironmentObjectDefinition type,
									  MapGraph mapGraph,
									  EntityBuilder builder,
									  JsonObject jsonObject,
									  Coords coord) {
		int dirIndex = jsonObject.get(DIRECTION).getAsInt();
		inflateEnvObstacleComponent(coord, type, builder, Direction.values()[dirIndex]);
		MapGraphNode node = mapGraph.getNode(coord.getCol(), coord.getRow());
		GameModelInstance mi = inflateEnvModelInstanceComponent(node, jsonObject, type, builder);
		inflateEnvLightComponent(builder, type, mi, dirIndex);
		node.setType(type.getNodeType());
		inflateDoor(type, builder, node);
		builder.addCollisionComponent();
	}

	private void inflateDoor(EnvironmentObjectDefinition type,
							 EntityBuilder builder,
							 MapGraphNode node) {
		if (type.getEnvironmentObjectType() == EnvironmentObjectType.DOOR) {
			builder.addDoorComponent(node);
			node.setDoor(EntityBuilder.getInstance().getCurrentEntity());

		}
	}

	private EnvironmentObjectDefinition inflateEnvType(String name, EnvironmentObjectType environmentType) {
		return Arrays.stream(environmentType.getDefinitions())
				.filter(d -> d.name().equalsIgnoreCase(name))
				.findFirst()
				.get();
	}

	private void inflateEnvironment(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		JsonArray envs = mapJsonObject.getAsJsonArray(KEY_ENVIRONMENT);
		envs.forEach(element -> {
			EntityBuilder builder = EntityBuilder.beginBuildingEntity(engine);
			JsonObject jsonObj = element.getAsJsonObject();
			Coords coord = new Coords(jsonObj.get(ROW).getAsInt(), jsonObj.get(COL).getAsInt());
			String envTypeName = jsonObj.get(ENV_TYPE).getAsString().toUpperCase();
			EnvironmentObjectType envType = EnvironmentObjectType.valueOf(envTypeName);
			EnvironmentObjectDefinition type = inflateEnvType(jsonObj.get(TYPE).getAsString(), envType);
			inflateEnvComponents(type, mapGraph, builder, jsonObj, coord);
			Entity entity = builder.finishAndAddToEngine();
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(entity).getModelInstance();
			Vector3 position = modelInstance.transform.getTranslation(auxVector3_1);
			RelativeBillboard relativeBillboard = type.getRelativeBillboard();
			Optional.ofNullable(relativeBillboard).ifPresent(r -> {
				TextureAtlas atlas = assetsManager.getAtlas(r.getBillboard());
				Array<TextureAtlas.AtlasRegion> f = atlas.findRegions(r.getBillboard().getName().toLowerCase());
				Direction dir = Direction.values()[jsonObj.get(DIRECTION).getAsInt()];
				float degrees = dir.getDirection(auxVector2_1).angleDeg() + ((dir == NORTH || dir == SOUTH) ? 180 : 0);
				Vector3 relativePosition = r.getRelativePosition(auxVector3_2).rotate(Vector3.Y, degrees);
				addRelativeBillboardEntity(position, r, f, relativePosition);
			});
		});
	}

	private void addRelativeBillboardEntity(final Vector3 position,
											final RelativeBillboard r,
											final Array<TextureAtlas.AtlasRegion> f,
											final Vector3 relativePosition) {
		EntityBuilder.beginBuildingEntity(engine)
				.addSimpleDecalComponent(position.add(relativePosition), f.get(0), true, true)
				.addAnimationComponent(r.getFrameDuration(), new Animation<>(r.getFrameDuration(), f, LOOP))
				.finishAndAddToEngine();
	}

	private void inflateLights(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		JsonArray lights = mapJsonObject.getAsJsonArray(KEY_LIGHTS);
		lights.forEach(element -> inflateLight(mapGraph, element));
	}

	private void inflateLight(final MapGraph mapGraph, final JsonElement element) {
		JsonObject lightJsonObject = element.getAsJsonObject();
		int row = lightJsonObject.get(ROW).getAsInt();
		int col = lightJsonObject.get(COL).getAsInt();
		Vector3 position = auxVector3_1.set(col + 0.5f, INDEPENDENT_LIGHT_HEIGHT, row + 0.5f);
		position.add(0, mapGraph.getNode(col, row).getHeight(), 0);
		EntityBuilder.beginBuildingEntity(engine)
				.addStaticLightComponent(position, INDEPENDENT_LIGHT_INTENSITY, INDEPENDENT_LIGHT_RADIUS, Color.WHITE)
				.finishAndAddToEngine();
	}

	private void inflatePickups(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		JsonArray pickups = mapJsonObject.getAsJsonArray(KEY_PICKUPS);
		pickups.forEach(element -> {
			JsonObject pickJsonObject = element.getAsJsonObject();
			PlayerWeaponsDefinitions type = PlayerWeaponsDefinitions.valueOf(pickJsonObject.get(TYPE).getAsString());
			TextureAtlas.AtlasRegion bulletRegion = null;
			if (!type.isMelee()) {
				bulletRegion = assetsManager.getAtlas(findByRelatedWeapon(type)).findRegion(REGION_NAME_BULLET);
			}
			inflatePickupEntity(pickJsonObject, type, bulletRegion, mapGraph);
		});
	}

	private void inflatePickupEntity(final JsonObject pickJsonObject,
									 final PlayerWeaponsDefinitions type,
									 final TextureAtlas.AtlasRegion bulletRegion,
									 final MapGraph mapGraph) {
		EntityBuilder builder = beginBuildingEntity(engine);
		inflatePickupModel(builder, pickJsonObject, type, mapGraph);
		builder.addPickUpComponentAsWeapon(type, assetsManager.getTexture(type.getSymbol()), bulletRegion)
				.finishAndAddToEngine();
	}

	private void inflatePickupModel(EntityBuilder builder,
									JsonObject pickJsonObject,
									PlayerWeaponsDefinitions type,
									MapGraph mapGraph) {
		Coords coord = new Coords(pickJsonObject.get(ROW).getAsInt(), pickJsonObject.get(COL).getAsInt());
		Models modelDefinition = type.getModelDefinition();
		String fileName = BOUNDING_BOX_PREFIX + modelDefinition.getFilePath();
		BoundingBox boundingBox = assetsManager.get(fileName, BoundingBox.class);
		GameModelInstance modelInstance = new GameModelInstance(assetsManager.getModel(modelDefinition), boundingBox);
		modelInstance.transform.setTranslation(auxVector3_1.set(coord.getCol() + 0.5f, 0, coord.getRow() + 0.5f));
		modelInstance.transform.translate(0, mapGraph.getNode(coord).getHeight(), 0);
		builder.addModelInstanceComponent(modelInstance, true);
	}

	private Vector3 inflateCharacterPosition(final JsonElement characterJsonElement, final MapGraph mapGraph) {
		JsonObject asJsonObject = characterJsonElement.getAsJsonObject();
		int col = asJsonObject.get(COL).getAsInt();
		int row = asJsonObject.get(ROW).getAsInt();
		float floorHeight = mapGraph.getNode(col, row).getHeight();
		return auxVector3_1.set(col + 0.5f, floorHeight + BILLBOARD_Y, row + 0.5f);
	}

	private void inflatePlayer(final JsonObject characterJsonObject, final MapGraph mapGraph) {
		CharacterAnimations general = assetsManager.get(PLAYER_GENERIC.name());
		EntityBuilder builder = beginBuildingEntity(engine).addPlayerComponent(general);
		Vector3 position = inflateCharacterPosition(characterJsonObject, mapGraph);
		auxCharacterSoundData.set(Sounds.PLAYER_PAIN, Sounds.PLAYER_DEATH, Sounds.STEP);
		CharacterSkillsParameters skills = new CharacterSkillsParameters(
				PLAYER_HEALTH,
				Agility.HIGH,
				Accuracy.LOW);
		CharacterData data = new CharacterData(
				position,
				Direction.values()[characterJsonObject.get(DIRECTION).getAsInt()],
				skills,
				auxCharacterSoundData);
		Atlases atlas = findByRelatedWeapon(DefaultGameSettings.STARTING_WEAPON);

		addCharBaseComponents(
				builder,
				data,
				CharacterTypes.PLAYER.getDefinitions()[0],
				atlas,
				DefaultGameSettings.STARTING_WEAPON.getWeaponsDefinition());

		builder.finishAndAddToEngine();
	}

	private CharacterSpriteData createCharacterSpriteData(final CharacterData data, final CharacterDefinition def) {
		CharacterSpriteData characterSpriteData = Pools.obtain(CharacterSpriteData.class);
		characterSpriteData.init(data.getDirection(),
				IDLE,
				def.getPrimaryAttackHitFrameIndex(),
				def.isSingleDeathAnimation());
		return characterSpriteData;
	}

	private void addCharBaseComponents(final EntityBuilder entityBuilder,
									   final CharacterData data,
									   final CharacterDefinition def,
									   final Atlases atlasDefinition,
									   WeaponsDefinitions primaryAttack) {
		CharacterSpriteData characterSpriteData = createCharacterSpriteData(data, def);
		Direction direction = data.getDirection();
		entityBuilder.addCharacterComponent(characterSpriteData, data.getSoundData(), data.getSkills(), primaryAttack)
				.addCharacterDecalComponent(assetsManager.get(atlasDefinition.name()), IDLE, direction, data.getPosition())
				.addCollisionComponent()
				.addAnimationComponent();
	}

	private void inflateEnemy(final JsonObject charJsonObject, final MapGraph mapGraph) {
		Enemies type = inflateEnemyType(charJsonObject);
		EntityBuilder b = beginBuildingEntity(engine).addEnemyComponent(type, inflateEnemyBulletFrames(type));
		Vector3 position = inflateCharacterPosition(charJsonObject, mapGraph);
		CharacterData data = inflateCharData(charJsonObject, type, position);
		addCharBaseComponents(b, data, type, type.getAtlasDefinition(), type.getPrimaryAttack());
		addEnemySkillFlower(type, b, position, mapGraph);
		initializeEnemy(position, b.finishAndAddToEngine());
	}

	private void initializeEnemy(Vector3 position, Entity enemy) {
		character.get(enemy).setTarget(engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first());
		initializeEnemyFlower(position, enemy);
	}

	private void addEnemySkillFlower(Enemies type, EntityBuilder builder, Vector3 position, MapGraph mapGraph) {
		Texture skillFlowerTexture = assetsManager.getTexture(SKILL_FLOWER_CENTER_IDLE);
		MapGraphNode node = mapGraph.getNode(position);
		position.y = node.getHeight() + type.getHeight() + EnemySystem.SKILL_FLOWER_HEIGHT_RELATIVE;
		builder.addSimpleDecalComponent(position, skillFlowerTexture, true, true);
		builder.addFlowerIconComponent();
	}

	private void initializeEnemyFlower(Vector3 position, Entity enemy) {
		SimpleDecalComponent simpleDecalComponent = enemy.getComponent(SimpleDecalComponent.class);
		List.of(SKILL_FLOWER_1,
				SKILL_FLOWER_2,
				SKILL_FLOWER_3,
				SKILL_FLOWER_4,
				SKILL_FLOWER_5,
				SKILL_FLOWER_6,
				SKILL_FLOWER_7,
				SKILL_FLOWER_8,
				ICON_IDLE).forEach(flower -> addFlowerRelatedDecal(simpleDecalComponent, flower, position));
	}

	private void addFlowerRelatedDecal(final SimpleDecalComponent simpleDecalComponent,
									   final UiTextures skillFlower,
									   final Vector3 position) {
		TextureRegion textureRegion = new TextureRegion(assetsManager.getTexture(skillFlower));
		RelatedDecal skillFlowerDecal = RelatedDecal.newDecal(textureRegion, true);
		skillFlowerDecal.setScale(BILLBOARD_SCALE);
		skillFlowerDecal.setPosition(position);
		simpleDecalComponent.addRelatedDecal(skillFlowerDecal);
	}

	private CharacterData inflateCharData(JsonObject characterJsonObject, Enemies type, Vector3 pos) {
		auxCharacterSoundData.set(type.getPainSound(), type.getDeathSound(), type.getStepSound());
		CharacterSkillsParameters skills = new CharacterSkillsParameters(
				type.getHealth(),
				type.getAgility(),
				type.getAccuracy() != null ? type.getAccuracy() : null);
		Direction direction = Direction.values()[characterJsonObject.get(DIRECTION).getAsInt()];
		return new CharacterData(pos, direction, skills, auxCharacterSoundData);
	}

	private Animation<TextureAtlas.AtlasRegion> inflateEnemyBulletFrames(Enemies type) {
		Animation<TextureAtlas.AtlasRegion> bulletAnimation = enemyBulletsTextureRegions.get(type);
		if (type.getPrimaryAttack() != null && !enemyBulletsTextureRegions.containsKey(type)) {
			String name = WeaponsDefinitions.RAPID_LASER_CANNON.name().toLowerCase();
			Array<TextureAtlas.AtlasRegion> regions = assetsManager.getAtlas(GUARD_BOT).findRegions(name);
			bulletAnimation = new Animation<>(type.getPrimaryAttack().getFrameDuration(), regions);
			enemyBulletsTextureRegions.put(type, bulletAnimation);
		}
		return bulletAnimation;
	}

	private Enemies inflateEnemyType(JsonObject characterJsonObject) {
		Enemies type;
		try {
			String asString = characterJsonObject.get(TYPE).getAsString();
			type = Optional.of(Arrays.stream(Enemies.values())
							.filter(def -> def.name().equalsIgnoreCase(asString))
							.findFirst())
					.orElseThrow()
					.get();
		} catch (Exception e) {
			int index = characterJsonObject.get(TYPE).getAsInt();
			type = Enemies.values()[index];
		}
		return type;
	}

	private void inflateCharacters(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		Arrays.stream(CharacterTypes.values()).forEach(type -> {
			String typeName = type.name().toLowerCase();
			JsonObject charactersJsonObject = mapJsonObject.getAsJsonObject(CHARACTERS);
			if (charactersJsonObject.has(typeName)) {
				JsonArray array = charactersJsonObject.get(typeName).getAsJsonArray();
				array.forEach(characterJsonElement -> {
					if (type == PLAYER) {
						inflatePlayer((JsonObject) characterJsonElement, mapGraph);
					} else if (type == ENEMY) {
						inflateEnemy((JsonObject) characterJsonElement, mapGraph);
					}
				});
			}
		});
	}

	private MapGraph createMapGraph(final JsonObject mapJsonObj) {
		JsonObject tilesJsonObject = mapJsonObj.get(TILES).getAsJsonObject();
		Dimension mapSize = new Dimension(tilesJsonObject.get(WIDTH).getAsInt(), tilesJsonObject.get(DEPTH).getAsInt());
		float ambient = GameUtils.getFloatFromJsonOrDefault(mapJsonObj, AMBIENT, 0);
		return new MapGraph(mapSize, engine, ambient);
	}

	private void inflateNode(final int row, final int col, final byte chr, MapGraphNode node) {
		SurfaceTextures definition = SurfaceTextures.values()[chr - 1];
		if (definition != MISSING) {
			GameModelInstance mi = new GameModelInstance(floorModel);
			defineNodeModelInstance(row, col, definition, mi);
			if (definition == Assets.SurfaceTextures.BLANK) {
				node.setType(OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			}
			inflateNodeEntity(node, definition, mi);
		}
	}

	private void inflateNodeEntity(MapGraphNode node, SurfaceTextures definition, GameModelInstance mi) {
		beginBuildingEntity(engine).addModelInstanceComponent(mi, true)
				.addFloorComponent(node, definition)
				.finishAndAddToEngine();
	}

	private void defineNodeModelInstance(int row,
										 int col,
										 SurfaceTextures definition,
										 GameModelInstance mi) {
		mi.materials.get(0).set(TextureAttribute.createDiffuse(assetsManager.getTexture(definition)));
		mi.transform.setTranslation(auxVector3_1.set(col + 0.5f, 0, row + 0.5f));
		mi.getAdditionalRenderData().setBoundingBox(mi.calculateBoundingBox(auxBoundingBox));
		mi.getAdditionalRenderData().setColorWhenOutside(Color.WHITE);
	}

	private Material createFloorMaterial( ) {
		Material material = new Material();
		material.id = "floor_test";
		return material;
	}

	private void createRect(final MeshPartBuilder meshPartBuilder) {
		meshPartBuilder.setUVRange(0, 0, 1, 1);
		final float OFFSET = -0.5f;
		meshPartBuilder.rect(
				auxVector3_4.set(OFFSET, 0, 1 + OFFSET),
				auxVector3_1.set(1 + OFFSET, 0, 1 + OFFSET),
				auxVector3_2.set(1 + OFFSET, 0, OFFSET),
				auxVector3_3.set(OFFSET, 0, OFFSET),
				auxVector3_5.set(0, 1, 0));
	}

	@Override
	public void dispose( ) {
		floorModel.dispose();
		wallCreator.dispose();
	}

	/**
	 * @param engine Destroys the current engine and replaces with the given one
	 */
	public void reset(final PooledEngine engine) {
		this.engine = engine;
	}
}
