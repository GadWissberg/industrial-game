package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
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
import com.gadarts.industrial.DebugSettings;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.PickUpComponent;
import com.gadarts.industrial.components.TriggerComponent;
import com.gadarts.industrial.components.character.CharacterData;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.player.PlayerComponent;
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
import com.gadarts.industrial.shared.model.characters.enemies.Enemies;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.shared.model.env.*;
import com.gadarts.industrial.shared.model.map.MapNodeData;
import com.gadarts.industrial.shared.model.map.NodeWalls;
import com.gadarts.industrial.shared.model.map.Wall;
import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;
import java.util.*;
import java.util.stream.IntStream;

import static com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP;
import static com.gadarts.industrial.components.ComponentsMapper.*;
import static com.gadarts.industrial.shared.assets.Assets.*;
import static com.gadarts.industrial.shared.assets.Assets.Atlases.GUARD_BOT;
import static com.gadarts.industrial.shared.assets.Assets.Atlases.PLAYER_GENERIC;
import static com.gadarts.industrial.shared.assets.Assets.SurfaceTextures.MISSING;
import static com.gadarts.industrial.shared.assets.MapJsonKeys.*;
import static com.gadarts.industrial.shared.model.characters.CharacterTypes.*;
import static com.gadarts.industrial.shared.model.characters.Direction.NORTH;
import static com.gadarts.industrial.shared.model.characters.Direction.SOUTH;
import static com.gadarts.industrial.shared.model.characters.SpriteType.IDLE;
import static com.gadarts.industrial.shared.model.env.LightConstants.*;
import static com.gadarts.industrial.shared.model.map.MapNodesTypes.OBSTACLE_KEY_DIAGONAL_FORBIDDEN;
import static com.gadarts.industrial.utils.EntityBuilder.beginBuildingEntity;
import static java.lang.String.format;

public class MapBuilder implements Disposable {
	public static final int PLAYER_HEALTH = 100;

	public static final String MAP_PATH_TEMP = "assets/maps/%s.json";
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
	private static final String KEY_TRIGGERS = "triggers";
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

	private static void inflateNodeHeight(JsonObject nodeDataJsonObject, MapGraphNode node) {
		if (!nodeDataJsonObject.has(HEIGHT)) return;
		float height = nodeDataJsonObject.get(HEIGHT).getAsFloat();
		node.setHeight(height);
		Entity entity = node.getEntity();
		if (entity != null && modelInstance.has(entity)) {
			modelInstance.get(entity).getModelInstance().transform.translate(0, height, 0);
		}
	}

	private static Vector3 inflateLightPosition(JsonObject lightJsonObj, int row, int col, MapGraph mapGraph) {
		float nodeHeight = mapGraph.getNode(col, row).getHeight();
		return auxVector3_1.set(
				col + 0.5f,
				lightJsonObj.has(HEIGHT) ? nodeHeight + lightJsonObj.get(HEIGHT).getAsFloat() : DEFAULT_LIGHT_HEIGHT,
				row + 0.5f);
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
		JsonObject nodesJsonObject = mapJsonObj.get(NODES).getAsJsonObject();
		MapGraph mapGraph = createMapGraph(mapJsonObj);
		inflateNodes(nodesJsonObject, mapGraph);
		inflateHeightsAndWalls(mapJsonObj, mapGraph, nodesJsonObject);
		inflateAllElements(mapJsonObj, mapGraph);
		mapGraph.init();
		markAllReachableNodes(mapGraph);
		return mapGraph;
	}

	private void markAllReachableNodes(MapGraph mapGraph) {
		Entity player = engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).get(0);
		MapGraphNode playerNode = mapGraph.getNode(characterDecal.get(player).getDecal().getPosition());
		GameHeuristic heuristic = new GameHeuristic();
		GamePathFinder auxPathPlanner = new GamePathFinder(mapGraph);
		MapGraphPath auxGraphPath = new MapGraphPath();
		mapGraph.getMapGraphStates().setIncludeCharactersInGetConnections(false);
		mapGraph.getMapGraphStates().setMaxConnectionCostInSearch(MapGraphConnectionCosts.CLEAN);
		mapGraph.getNodes().forEach(node -> {
			boolean reachable = auxPathPlanner.searchNodePath(playerNode, node, heuristic, auxGraphPath);
			node.setReachable(reachable);
		});
	}

	private void inflateNodes(JsonObject nodesJsonObject, MapGraph mapGraph) {
		byte[] matrixByte = Base64.getDecoder().decode(nodesJsonObject.get(MATRIX).getAsString().getBytes());
		IntStream.range(0, mapGraph.getDepth()).forEach(row ->
				IntStream.range(0, mapGraph.getWidth()).forEach(col -> {
					byte currentValue = matrixByte[row * mapGraph.getWidth() + col % mapGraph.getDepth()];
					if (currentValue != 0) {
						MapGraphNode node = mapGraph.getNode(col, row);
						inflateNode(row, col, currentValue, node);
					}
				}));
	}

	private MapGraphNode getNodeByJson(final MapGraph mapGraph, final JsonObject tileJsonObject) {
		int row = tileJsonObject.get(ROW).getAsInt();
		int col = tileJsonObject.get(COL).getAsInt();
		return mapGraph.getNode(col, row);
	}

	private void inflateHeightsAndWalls(JsonObject mapJsonObject,
										MapGraph mapGraph,
										JsonObject nodesJsonObject) {
		JsonArray nodesData = nodesJsonObject.getAsJsonArray(NODES_DATA);
		nodesData.forEach(nodeDataJson -> {
			JsonObject nodeDataJsonObject = nodeDataJson.getAsJsonObject();
			MapGraphNode node = getNodeByJson(mapGraph, nodeDataJsonObject);
			inflateNodeHeight(nodeDataJsonObject, node);
		});
		nodesData.forEach(nodeDataJson -> {
			JsonObject nodeDataJsonObject = nodeDataJson.getAsJsonObject();
			MapGraphNode node = getNodeByJson(mapGraph, nodeDataJsonObject);
			if (nodeDataJsonObject.has(WALLS)) {
				inflateWalls(nodeDataJsonObject.getAsJsonObject(WALLS), node, node.getHeight(), mapGraph);
			}
		});
		JsonElement heightsElement = mapJsonObject.get(NODES).getAsJsonObject().get(HEIGHTS);
		Optional.ofNullable(heightsElement).ifPresent(element -> {
			JsonArray heights = element.getAsJsonArray();
			heights.forEach(jsonElement -> {
				JsonObject tileJsonObject = jsonElement.getAsJsonObject();
				MapGraphNode node = getNodeByJson(mapGraph, tileJsonObject);
				float height = tileJsonObject.get(HEIGHT).getAsFloat();
				node.setHeight(height);
				Entity entity = node.getEntity();
				if (entity != null && modelInstance.has(entity)) {
					modelInstance.get(entity).getModelInstance().transform.translate(0, height, 0);
				}
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

	private void inflateEastWall(final JsonObject nodeWallsJsonObject,
								 final MapGraphNode node,
								 final float height,
								 final MapGraph mapGraph) {
		int col = node.getCol();
		int eastCol = col + 1;
		JsonElement east = nodeWallsJsonObject.get(EAST);
		if (eastCol < mapGraph.getWidth()) {
			if (height != mapGraph.getNode(eastCol, node.getRow()).getHeight() && east != null) {
				JsonObject asJsonObject = east.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applyEastWall(node, height, mapGraph, wallParameters, eastCol);
			}
		}
	}

	private void inflateSouthWall(final JsonObject nodeWallsJsonObject,
								  final MapGraphNode node,
								  final float height,
								  final MapGraph mapGraph) {
		int row = node.getRow();
		int southRow = row + 1;
		JsonElement south = nodeWallsJsonObject.get(MapJsonKeys.SOUTH);
		if (southRow < mapGraph.getDepth()) {
			if (height != mapGraph.getNode(node.getCol(), southRow).getHeight() && south != null) {
				JsonObject asJsonObject = south.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applySouthWall(node, height, mapGraph, wallParameters, southRow);
			}
		}
	}

	private void inflateNorthWall(final JsonObject nodeWallsJsonObject,
								  final MapGraphNode node,
								  final float height,
								  final MapGraph mapGraph) {
		int row = node.getRow();
		int northRow = row - 1;
		JsonElement north = nodeWallsJsonObject.get(MapJsonKeys.NORTH);
		if (northRow >= 0) {
			if (height != mapGraph.getNode(node.getCol(), northRow).getHeight() && north != null) {
				JsonObject asJsonObject = north.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applyNorthWall(node, height, mapGraph, wallParameters, northRow);
			}
		}
	}

	private void inflateWestWall(final JsonObject nodeWallsJsonObject,
								 final MapGraphNode node,
								 final float height,
								 final MapGraph mapGraph) {
		int col = node.getCol();
		int westCol = col - 1;
		JsonElement west = nodeWallsJsonObject.get(WEST);
		if (westCol >= 0) {
			if (height != mapGraph.getNode(westCol, node.getRow()).getHeight() && west != null) {
				JsonObject asJsonObject = west.getAsJsonObject();
				WallParameters wallParameters = inflateWallParameters(asJsonObject);
				applyWestWall(node, height, mapGraph, wallParameters, westCol);
			}
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

	private void inflateWalls(final JsonObject nodeWallsJsonObject,
							  final MapGraphNode node,
							  final float height,
							  final MapGraph mapGraph) {
		inflateEastWall(nodeWallsJsonObject, node, height, mapGraph);
		inflateSouthWall(nodeWallsJsonObject, node, height, mapGraph);
		inflateWestWall(nodeWallsJsonObject, node, height, mapGraph);
		inflateNorthWall(nodeWallsJsonObject, node, height, mapGraph);
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
							   WallParameters wallParameters,
							   int westNodeCol) {
		SurfaceTextures definition = wallParameters.getDefinition();
		if (definition != MISSING) {
			MapNodeData nodeData = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			NodeWalls walls = nodeData.getWalls();
			walls.setWestWall(WallCreator.createWall(
					nodeData,
					wallCreator.getWestWallModel(),
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
			wallCreator.adjustWestWall(
					westNodeData,
					nodeData);
			inflateWall(walls.getWestWall(), nodeData, mapGraph);
		}
	}

	private void applyEastWall(MapGraphNode node,
							   float height,
							   MapGraph mapGraph,
							   WallParameters wallParameters,
							   int eastNodeCol) {
		SurfaceTextures definition = wallParameters.getDefinition();
		if (definition != MISSING) {
			MapNodeData nodeData = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			NodeWalls walls = nodeData.getWalls();
			walls.setEastWall(WallCreator.createWall(
					nodeData,
					wallCreator.getEastWallModel(),
					assetsManager,
					definition));
			Coords coords = nodeData.getCoords();
			MapNodeData eastNodeData = new MapNodeData(
					coords.getRow(),
					eastNodeCol,
					OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			nodeData.lift(height);
			if (eastNodeCol >= 0 && eastNodeCol < mapGraph.getWidth()) {
				eastNodeData.setHeight(mapGraph.getNode(eastNodeData.getCoords()).getHeight());
			}
			wallCreator.adjustEastWall(nodeData, eastNodeData);
			inflateWall(walls.getEastWall(), nodeData, mapGraph);
		}
	}

	private void applySouthWall(MapGraphNode node,
								float height,
								MapGraph mapGraph,
								WallParameters wallParameters,
								int southNodeRow) {
		SurfaceTextures definition = wallParameters.getDefinition();
		if (definition != MISSING) {
			MapNodeData nodeData = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			NodeWalls walls = nodeData.getWalls();
			walls.setSouthWall(WallCreator.createWall(
					nodeData,
					wallCreator.getSouthWallModel(),
					assetsManager,
					definition));
			Coords coords = nodeData.getCoords();
			MapNodeData southNodeData = new MapNodeData(
					southNodeRow,
					coords.getCol(),
					OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			nodeData.lift(height);
			if (southNodeRow >= 0 && southNodeRow < mapGraph.getDepth()) {
				southNodeData.setHeight(mapGraph.getNode(southNodeData.getCoords()).getHeight());
			}
			wallCreator.adjustSouthWall(southNodeData, nodeData);
			inflateWall(walls.getSouthWall(), nodeData, mapGraph);
		}
	}

	private void applyNorthWall(MapGraphNode node,
								float height,
								MapGraph mapGraph,
								WallParameters wallParameters,
								int northNodeRow) {
		SurfaceTextures definition = wallParameters.getDefinition();
		if (definition != MISSING) {
			MapNodeData nodeData = new MapNodeData(node.getRow(), node.getCol(), OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			NodeWalls walls = nodeData.getWalls();
			walls.setNorthWall(WallCreator.createWall(
					nodeData,
					wallCreator.getNorthWallModel(),
					assetsManager,
					definition));
			Coords coords = nodeData.getCoords();
			MapNodeData northNodeData = new MapNodeData(
					northNodeRow,
					coords.getCol(),
					OBSTACLE_KEY_DIAGONAL_FORBIDDEN);
			nodeData.lift(height);
			if (northNodeRow >= 0 && northNodeRow < mapGraph.getDepth()) {
				northNodeData.setHeight(mapGraph.getNode(northNodeData.getCoords()).getHeight());
			}
			wallCreator.adjustNorthWall(nodeData, northNodeData);
			inflateWall(walls.getNorthWall(), nodeData, mapGraph);
		}
	}

	private void inflateAllElements(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		inflateCharacters(mapJsonObject, mapGraph);
		inflateLights(mapJsonObject, mapGraph);
		inflateTriggers(mapJsonObject, mapGraph);
		inflateEnvironment(mapJsonObject, mapGraph);
		inflatePickups(mapJsonObject, mapGraph);
	}

	private void inflateEnvObjectComponent(final Coords coord,
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
		builder.addEnvironmentObjectComponent(topLeft, bottomRight, type);
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
		Vector3 offset = type.getOffset(auxVector3_1);
		modelInstance.transform.translate(0, height, 0);
		if (!offset.isZero()) {
			modelInstance.nodes.forEach(n -> {
				n.isAnimated = false;
				n.translation.add(offset);
			});
			modelInstance.calculateTransforms();
		}
		if (type instanceof ThingsDefinitions) {
			ThingsDefinitions.handleEvenSize((ThingsDefinitions) type, modelInstance, direction);
		}
		GeneralUtils.applyExplicitModelTexture(def, modelInstance, assetsManager);
		return modelInstance;
	}

	private GameModelInstance inflateEnvModelInstanceComponent(final MapGraphNode node,
															   final JsonObject envJsonObj,
															   final EnvironmentObjectDefinition type,
															   final EntityBuilder builder) {
		float height = envJsonObj.get(HEIGHT).getAsFloat();
		GameModelInstance mi = inflateEnvironmentModelInstance(node, envJsonObj.get(DIRECTION).getAsInt(), type, height);
		mi.getAdditionalRenderData().setColorWhenOutside(Color.WHITE);
		builder.addModelInstanceComponent(mi, true);
		Optional.ofNullable(type.getAppendixModelDefinition())
				.ifPresent(a -> {
					Vector3 offset = type.getOffset(auxVector3_1);
					GameModelInstance appendixModelInstance = new GameModelInstance(assetsManager.getModel(a));
					appendixModelInstance.transform.set(mi.transform);
					appendixModelInstance.transform.translate(offset);
					builder.addAppendixModelInstanceComponent(appendixModelInstance);
					GeneralUtils.applyExplicitModelTexture(a, appendixModelInstance, assetsManager);
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
			builder.addStaticLightComponent(position, l.getIntensity(), l.getRadius(), l.getColor());
		});
	}

	private void inflateEnvComponents(EnvironmentObjectDefinition type,
									  MapGraph mapGraph,
									  EntityBuilder builder,
									  JsonObject jsonObject,
									  Coords coord) {
		int dirIndex = jsonObject.get(DIRECTION).getAsInt();
		inflateEnvObjectComponent(coord, type, builder, Direction.values()[dirIndex]);
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
			builder.addDoorComponent(node, ((DoorsDefinitions) type));
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
		lights.forEach(e -> inflateLight(e, mapGraph));
	}

	private void inflateTriggers(JsonObject mapJsonObject, MapGraph mapGraph) {
		JsonArray triggers = mapJsonObject.getAsJsonArray(KEY_TRIGGERS);
		triggers.forEach(element -> inflateTrigger(mapGraph, element));
	}

	private void inflateLight(final JsonElement element, MapGraph mapGraph) {
		JsonObject lightJsonObj = element.getAsJsonObject();
		int row = lightJsonObj.get(ROW).getAsInt();
		int col = lightJsonObj.get(COL).getAsInt();
		Vector3 position = inflateLightPosition(lightJsonObj, row, col, mapGraph);
		float i = lightJsonObj.has(INTENSITY) ? lightJsonObj.get(INTENSITY).getAsFloat() : DEFAULT_LIGHT_INTENSITY;
		float radius = lightJsonObj.has(RADIUS) ? lightJsonObj.get(RADIUS).getAsFloat() : DEFAULT_LIGHT_RADIUS;
		EntityBuilder.beginBuildingEntity(engine)
				.addStaticLightComponent(position, i, radius)
				.finishAndAddToEngine();
	}

	private void inflateTrigger(final MapGraph mapGraph, final JsonElement element) {
		JsonObject triggerJsonObject = element.getAsJsonObject();
		int row = triggerJsonObject.get(ROW).getAsInt();
		int col = triggerJsonObject.get(COL).getAsInt();
		TriggerComponent component = engine.createComponent(TriggerComponent.class);
		mapGraph.getNode(col, row).getEntity().add(component);
	}

	private void inflatePickups(final JsonObject mapJsonObject, final MapGraph mapGraph) {
		JsonArray pickups = mapJsonObject.getAsJsonArray(KEY_PICKUPS);
		pickups.forEach(element -> {
			JsonObject pickJsonObject = element.getAsJsonObject();
			PlayerWeaponsDefinitions type = PlayerWeaponsDefinitions.valueOf(pickJsonObject.get(TYPE).getAsString());
			TextureAtlas.AtlasRegion bulletRegion = null;
			if (!type.isMelee()) {
				bulletRegion = assetsManager.getAtlas(type.getRelatedAtlas()).findRegion(REGION_NAME_BULLET);
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
				.addSimpleShadowComponent(PickUpComponent.SIMPLE_SHADOW_RADIUS)
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
		auxCharacterSoundData.set(Sounds.PLAYER_PAIN, Sounds.PLAYER_DEATH);
		CharacterSkillsParameters skills = new CharacterSkillsParameters(
				!DebugSettings.LOW_HP_FOR_PLAYER ? PLAYER_HEALTH : 1,
				PlayerComponent.PLAYER_AGILITY,
				Accuracy.LOW);
		CharacterData data = new CharacterData(
				position,
				Direction.values()[characterJsonObject.get(DIRECTION).getAsInt()],
				skills,
				auxCharacterSoundData);
		Atlases atlas = DebugSettings.STARTING_WEAPON.getRelatedAtlas();

		addCharBaseComponents(
				builder,
				data,
				CharacterTypes.PLAYER.getDefinitions()[0],
				atlas,
				DebugSettings.STARTING_WEAPON.getWeaponsDefinition());

		builder.finishAndAddToEngine();
	}

	private CharacterSpriteData createCharacterSpriteData(final CharacterDefinition def) {
		CharacterSpriteData characterSpriteData = Pools.obtain(CharacterSpriteData.class);
		characterSpriteData.init(
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
		CharacterSpriteData characterSpriteData = createCharacterSpriteData(def);
		Direction direction = data.direction();
		float radius = def.getShadowRadius();
		CharacterSoundData soundData = data.soundData();
		entityBuilder.addCharacterComponent(characterSpriteData, soundData, data.skills(), primaryAttack, direction)
				.addCharacterDecalComponent(assetsManager.get(atlasDefinition.name()), IDLE, direction, data.position())
				.addCollisionComponent()
				.addSimpleShadowComponent(radius)
				.addAnimationComponent();
	}

	private void inflateEnemy(final JsonObject charJsonObject, final MapGraph mapGraph) {
		Enemies type = inflateEnemyType(charJsonObject);
		EntityBuilder b = beginBuildingEntity(engine).addEnemyComponent(type, inflateEnemyBulletFrames(type));
		Vector3 position = inflateCharacterPosition(charJsonObject, mapGraph);
		CharacterData data = inflateEnemyCharData(charJsonObject, type, position);
		addCharBaseComponents(b, data, type, type.getAtlasDefinition(), type.getPrimaryAttack());
		Entity entity = b.finishAndAddToEngine();
		character.get(entity).setTarget(engine.getEntitiesFor(Family.all(PlayerComponent.class).get()).first());
	}

	private CharacterData inflateEnemyCharData(JsonObject characterJsonObject, Enemies type, Vector3 pos) {
		auxCharacterSoundData.set(type.getPainSound(), type.getDeathSound());
		CharacterSkillsParameters skills = new CharacterSkillsParameters(
				!DebugSettings.LOW_HP_FOR_ENEMIES ? type.getHealth() : 1,
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
		JsonObject tilesJsonObject = mapJsonObj.get(NODES).getAsJsonObject();
		Dimension mapSize = new Dimension(tilesJsonObject.get(WIDTH).getAsInt(), tilesJsonObject.get(DEPTH).getAsInt());
		float ambient = GameUtils.getFloatFromJsonOrDefault(mapJsonObj, AMBIENT, 0);
		return new MapGraph(mapSize, engine, ambient);
	}

	private void inflateNode(final int row, final int col, final byte chr, MapGraphNode node) {
		SurfaceTextures definition = SurfaceTextures.values()[chr - 1];
		EntityBuilder entityBuilder = beginBuildingEntity(engine);
		if (definition != MISSING) {
			GameModelInstance mi = new GameModelInstance(floorModel);
			defineNodeModelInstance(row, col, definition, mi);
			entityBuilder.addModelInstanceComponent(mi, true);
		}
		node.setType(definition == Assets.SurfaceTextures.BLANK ? OBSTACLE_KEY_DIAGONAL_FORBIDDEN : node.getType());
		node.setEntity(entityBuilder.addFloorComponent(node, definition).finishAndAddToEngine());
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
		BlendingAttribute blendingAttribute = new BlendingAttribute();
		blendingAttribute.opacity = 1F;
		material.set(blendingAttribute);
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
