/* ************************************
 * Ujjval Tandel and Anshul Shandilya
 * CSC 165-02: Project 3 Beantastic
 * 12/15/2020
 * ***********************************/


package Core;

import static ray.rage.scene.SkeletalEntity.EndType.LOOP;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.*;
import java.util.*;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;

import java.io.*;
import java.util.*;
import ray.rage.*;
import ray.rage.game.*;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.Renderable.*;
import ray.rage.scene.*;
import ray.rage.scene.Camera.Frustum.*;
import ray.rage.scene.controllers.*;
import ray.rml.*;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.rendersystem.states.*;
import ray.rage.asset.material.Material;
import ray.rage.asset.texture.*;
import ray.input.*;
import ray.input.action.*;
import ray.networking.IGameConnection.ProtocolType;
import ray.physics.PhysicsObject;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsEngineFactory;
import ray.rage.rendersystem.shader.*;
import ray.rage.util.*;
import GameEngine.*;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import ray.audio.*;
import com.jogamp.openal.ALFactory;

import java.util.Vector;


//Class declaration for BeantasticGame
public class BeantasticGame extends VariableFrameRateGame {

	GL4RenderSystem rs;
	float elapsTime = 0.0f;
	String elapsTimeStr, inputName, hud;
    int elapsTimeSec, counter = 0;
    Random randomNumber = new Random();
    
    //Variables to limit the number of certain game objects for the game
    final static int maxCrystal = 10;
    final static int maxOre = 10;
    
    final static int maxRocks = 50;
    
    //Private variables for the class BeantasticGame-----------------------------------------------------------------------------------------------------------------
    private InputManager im;
    private SceneManager sm;
    private Action moveForwardAction, moveBackwardAction, moveLeftAction, moveRightAction, moveCameraAction, moveDirectionAction, moveUpDownAction, rotateRightA, rotateLeftA, colorA, rotateAction, rotatePlayerAction;
    public SceneNode cameraNode;
	private SceneNode gameWorldObjectsNode;
	private SceneNode playerObjectNode, manualObjectsNode, shipObjectNode, npcObjectNode, planetNode;
	private ArrayList<SceneNode> oreObjectList = new ArrayList<SceneNode>(), crystalObjectList = new ArrayList<SceneNode>(), rockObjectList = new ArrayList<SceneNode>();
    private Camera3pController playerController;	
    private static final String SKYBOX_NAME = "SkyBox";
    private boolean skyBoxVisible = true;
    
	//server variables---
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected;
	private Vector<UUID> gameObjectsToRemove;
	private boolean ghostCheckN;
    
    //Physics variables
	private SceneNode ball1Node, ball2Node, groundNode, rockNode;
    private SceneNode cameraPositionNode;
    private final static String GROUND_E = "Ground";
    private final static String GROUND_N = "GroundNode";
    private PhysicsEngine physicsEng; 
    private PhysicsObject ball1PhysObj, ball2PhysObj, gndPlaneP, rockPhysObj, flagPhysObj;
    
    //Animation variables
    private boolean running = false;
    private boolean walkB, idleB, walkG2;														//Animation
	
    //Sound variables
    private IAudioManager audioManager;  
    private Sound stepSound, bgSound, sparkSound;
	
    //Public variables for the class BeantasticGame------------------------------------------------------------------------------------------------------------------
    public Camera camera;
    public SceneNode playerNode, shipNode, npcNode, flagNode;		
    public ArrayList<SceneNode> oreNodeList = new ArrayList<SceneNode>(), crystalNodeList = new ArrayList<SceneNode>(), rockNodeList = new ArrayList<SceneNode>();
    public Light plight2;
    //score 
    private int oresCount;
    private String winner = "";
    
    //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    //Debug variables for the game developer to make things easier
    //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    public boolean debugCamera = false;																					//Sets the player speed superman levels for easier to get from one place to another
    
    //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    
    //Protected Variables--------------------------------------------------------------------------------------------------------------------------------------------
    
    //script variables----
    protected ScriptEngine jsEngine;
    protected File scriptFile;
    
    
    //HUD
    String dispStr;
    //Constructor for the class BeantasticGame
    public BeantasticGame(String serverAddr, int sPort) {
    	
        super();
        this.serverAddress = serverAddr;
        this.serverPort = sPort;
        this.serverProtocol = ProtocolType.UDP;
        isClientConnected = false;
        
        walkB=false;
        idleB=false;
        walkG2 = false;
        oresCount = 0;
    }
    
    //Main function for the game
    public static void main(String[] args) {
    	
        Game game = new BeantasticGame(args[0], Integer.parseInt(args[1]));
        
        try {
        	
            game.startup();
            game.run();
            
        } catch (Exception e) {
        	
            e.printStackTrace(System.err);
            
        } finally {
        	
            game.shutdown();
            game.exit();
            
        }
        
    }
    
    //Game implementation starts here------------------------------------------------------------------------------------------------------------------------------------------
    
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Code for setting up the windows, cameras, scenes, objects, textures for the game-----------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    
	@Override
	protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
		rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
		rs.getRenderWindow().setTitle("Beantastic");
		
		//rs.getRenderWindow().setIconImage();
	}

    @Override
    protected void setupCameras(SceneManager sm, RenderWindow rw) {
    	
    	SceneNode rootNode = sm.getRootSceneNode();
    	camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);
    	rw.getViewport(0).setCamera(camera);
    	cameraNode = rootNode.createChildSceneNode("MainCameraNode");
    	cameraNode.attachObject(camera);
    	camera.setMode('n');
    	camera.getFrustum().setFarClipDistance(1000.0f);
    	
    }
    
	private void setupOrbitCameras(Engine eng, SceneManager sm) {
		
		String msName = im.getMouseName();
		playerController = new Camera3pController(camera, cameraNode, playerNode, msName, im, this);
		
	}
	
    @Override
    protected void setupScene(Engine eng, SceneManager sm) throws IOException {
    	
    	setupNetworking();
    	System.out.println("Collect all the ores!");
    	//physics demonstration
    	SceneNode rootNode = sm.getRootSceneNode();
    	
    	Entity ball1Entity = sm.createEntity("ball1", "rock.obj"); 
    	ball1Node = rootNode.createChildSceneNode("Ball1Node");
    	ball1Node.attachObject(ball1Entity);
    	ball1Node.setLocalPosition(0, 45, -2);
    	ball1Node.setLocalScale(0.4f, 0.5f, 0.4f);
    	TextureManager texRock = eng.getTextureManager();
    	Texture moonRock = texRock.getAssetByPath("redMoon.jpg");
        RenderSystem rsd0 = sm.getRenderSystem();
        TextureState stated0 =  (TextureState) rsd0.createRenderState(RenderState.Type.TEXTURE);
        stated0.setTexture(moonRock);
        ball1Entity.setRenderState(stated0);
        ball1Node.yaw(Degreef.createFrom(180.0f));

    	Entity groundEntity = sm.createEntity(GROUND_E, "cube.obj");
    	groundNode = rootNode.createChildSceneNode(GROUND_N);
    	groundNode.attachObject(groundEntity);
    	groundNode.setLocalPosition(0, -2.5f, 0f);//2f
    	groundEntity.setVisible(false);
    	im = new GenericInputManager();
      //networking call		
    	//Initializing the input manager
    	getInput();																									//Determine the type of input device
        gameWorldObjectsNode = sm.getRootSceneNode().createChildSceneNode("GameWorldObjectsNode");			        //Initializing the gameWorldObjects Scene Node
        manualObjectsNode = gameWorldObjectsNode.createChildSceneNode("ManualObjectsNode");							//Initializing the manualObjects scene node 
        
		//Creating the player node to add in the game, upgrade from last only entity approach
		  playerObjectNode = sm.getRootSceneNode().createChildSceneNode("PlayerNode");
		
        //Creating a player
        //Entity playerEntity = sm.createEntity("myPlayer", "astro.obj");
        //playerEntity.setPrimitive(Primitive.TRIANGLES);
		SkeletalEntity playerEntity = sm.createSkeletalEntity("myPlayer", "astroRig.rkm", "astro.rks");
		playerEntity.setPrimitive(Primitive.TRIANGLES);
        playerNode = playerObjectNode.createChildSceneNode(playerEntity.getName() + "Node");
        playerNode.attachObject(playerEntity);
        playerNode.scale(.06f, .06f, .06f);
       // playerNode.translate(0,0.5f,0);
        //player texture
        TextureManager tmd1 = eng.getTextureManager();
        Texture assetd1 = tmd1.getAssetByPath("astroTex.png");
        RenderSystem rsd1 = sm.getRenderSystem();
        TextureState stated1 =  (TextureState) rsd1.createRenderState(RenderState.Type.TEXTURE);
        stated1.setTexture(assetd1);
        playerEntity.setRenderState(stated1);
        //playerNode.yaw(Degreef.createFrom(180.0f));
        
        //animations----
        playerEntity.loadAnimation("walk", "walk2.rka");
        
        //Idle is not used currently
        //playerEntity.loadAnimation("idle", "idle.rka");
        
        //Terrain
      		Tessellation tessE = sm.createTessellation("tessE", 6);
      		tessE.setSubdivisions(8f);
      		SceneNode tessN = (SceneNode) sm.getRootSceneNode().createChildNode("TessN");
      		tessN.attachObject(tessE);	
      		//tessN.scale(200, 100, 200);
      		tessN.scale(130, 230, 130);
      		//tessN.translate(Vector3f.createFrom(-6.2f, -2.2f, 2.7f));
      		//tessN.yaw(Degreef.createFrom(37.2f));
      		tessN.setLocalPosition(0f, -2.2f, 0f);
      		//tessN.setLocalPosition(0f, 0f, 0f);
      		tessE.setHeightMap(this.getEngine(), "testTerr.png");
      		//https://freestocktextures.com/texture/turquoise-blue-water,941.html
      		tessE.setTexture(this.getEngine(), "water.jpg");
      	//TERRAIN END
      		
        //npc building
        SkeletalEntity npcEntity = sm.createSkeletalEntity("npc", "astroRig.rkm", "astro.rks");
        Texture texNpc = sm.getTextureManager().getAssetByPath("npcTex.png");
        TextureState tstateNpc = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        tstateNpc.setTexture(texNpc);
        npcEntity.setRenderState(tstateNpc);
        npcObjectNode = gameWorldObjectsNode.createChildSceneNode("NpcNode");
        npcNode = npcObjectNode.createChildSceneNode(npcEntity.getName() + "Node");
        npcNode.attachObject(npcEntity);
        npcNode.scale(.8f, .8f, .8f);
        npcNode.translate(-20f, 5.5f, -10f);
        npcEntity.loadAnimation("idle", "idle.rka");
    	npcEntity.playAnimation("idle", 1.5f, LOOP, 0);
    	
    	//npc 
    	protClient.askForNPC();
   		//update the vertical position of the objects acc. to the terrain ***FILL IN THE INTEGER_TYPE VALUE FOR TYPE OF OBJECT YOU WANT TO UPDATE 1:npc, 2:spaceship, 3:ore, 4:crystal, 5:rock
    	updateObjectVerticalPosition(npcNode, 1);
        //spaceship----
		  shipObjectNode = (SceneNode) gameWorldObjectsNode.createChildNode("shipNode");
        Entity shipEntity = sm.createEntity("myShip", "spaceship.obj");
        shipEntity.setPrimitive(Primitive.TRIANGLES);
        shipNode = shipObjectNode.createChildSceneNode(shipEntity.getName() + "Node");
        shipNode.attachObject(shipEntity);
        shipNode.setLocalPosition(0, -0.6f, 0);
    	shipNode.setLocalScale(2f, 2f, 2f);
   		//update the vertical position of the objects acc. to the terrain ***FILL IN THE INTEGER_TYPE VALUE FOR TYPE OF OBJECT YOU WANT TO UPDATE 1:npc, 2:spaceship, 3:ore, 4:crystal, 5:rock
    	updateObjectVerticalPosition(shipNode, 2);
        TextureManager shipTM = eng.getTextureManager();
        //change cube.png is spaceship texture
        Texture shipA = shipTM.getAssetByPath("cube.png");
        RenderSystem shipR = sm.getRenderSystem();
        TextureState shipS = (TextureState) shipR.createRenderState(RenderState.Type.TEXTURE);
        shipS.setTexture(shipA);
        shipEntity.setRenderState(shipS);
        
        //Script
        ScriptEngineManager factory = new ScriptEngineManager();
      	java.util.List<ScriptEngineFactory> list = factory.getEngineFactories();
      	jsEngine = factory.getEngineByName("js");
      	File scriptFile = new File("limit.js");
        //this.executeScript(jsEngine, scriptFile);
        this.runScript(scriptFile);
       
        
        

        //Setting ores objects for the game world
        for(int i = 0; i < maxOre; i++) {
        	
        	SceneNode tempOreObjectNode, tempOreNode;
	   		tempOreObjectNode = (SceneNode) gameWorldObjectsNode.createChildNode("oreNode" + i);
	   		Entity oreEntity = sm.createEntity("myOre" + i, "ore.obj");
	   		oreEntity.setPrimitive(Primitive.TRIANGLES); 
	   		tempOreNode = tempOreObjectNode.createChildSceneNode(oreEntity.getName() + "Node");
	   		tempOreNode.attachObject(oreEntity);
	   		tempOreNode.setLocalScale(0.5f, 0.5f, 0.5f); 
            tempOreNode.translate(5,5,5);
	   		tempOreNode.setLocalPosition(randomNumber.nextInt(100)-50, -.7f, randomNumber.nextInt(100)-50);			//Set random position

	   		//Setting the rotation controller
	   		RotationController rcOre = new RotationController(Vector3f.createUnitVectorY(), .3f); 												//Rotation for the ore model in the game 
	   		rcOre.addNode(tempOreNode); 
	   		sm.addController(rcOre);
	   		

	   		//Filling the respective arrays
	   		oreObjectList.add(tempOreObjectNode);
	   		oreNodeList.add(tempOreNode);
	   	//update the vertical position of the objects acc. to the terrain ***FILL IN THE INTEGER_TYPE VALUE FOR TYPE OF OBJECT YOU WANT TO UPDATE 1:npc, 2:spaceship, 3:ore, 4:crystal, 5:rock
	    	updateObjectVerticalPosition(tempOreNode, 3);
	   		
        }
        
        //Setting crystal objects for the game world
        for(int i = 0; i < maxCrystal; i++) {
        
        	SceneNode tempCrystalObjectNode, tempCrystalNode;
    		tempCrystalObjectNode = (SceneNode) gameWorldObjectsNode.createChildNode("crystalNode" + i);
            Entity crystalEntity = sm.createEntity("myCrystal" + i, "crystal.obj");
            crystalEntity.setPrimitive(Primitive.TRIANGLES);
            tempCrystalNode = tempCrystalObjectNode.createChildSceneNode(crystalEntity.getName() + "Node");
            tempCrystalNode.attachObject(crystalEntity);
            tempCrystalNode.setLocalPosition(randomNumber.nextInt(100)-50,-1.7f, randomNumber.nextInt(100)-50);
            tempCrystalNode.setLocalScale(0.4f, 0.6f, 0.4f);
	   		//Filling the respective arrays
	   		crystalObjectList.add(tempCrystalObjectNode);
	   		crystalNodeList.add(tempCrystalNode);
	   		//update the vertical position of the objects acc. to the terrain ***FILL IN THE INTEGER_TYPE VALUE FOR TYPE OF OBJECT YOU WANT TO UPDATE 1:npc, 2:spaceship, 3:ore, 4:crystal, 5:rock
	   		updateObjectVerticalPosition(tempCrystalNode, 4);
        	
        }
       
        //Setting Rocks objects for the game world
        for(int i = 0; i < maxRocks; i++) {
        
        	int smallCounter = 0, mediumCounter = 0, largeCounter = 0;
        	SceneNode tempRockObjectNode, tempRockNode;
	   		tempRockObjectNode = (SceneNode) gameWorldObjectsNode.createChildNode("rockNode" + i);
	   		Entity rockEntity = sm.createEntity("myRock" + i, "rock.obj");
	   		rockEntity.setPrimitive(Primitive.TRIANGLES); 
	   		tempRockNode = tempRockObjectNode.createChildSceneNode(rockEntity.getName() + "Node");
	   		tempRockNode.attachObject(rockEntity);
	   		tempRockNode.setLocalPosition(0f, 5f, 0f);
	   		if(smallCounter <= 12 && smallCounter + mediumCounter + largeCounter != 50) {
	   			
	   			tempRockNode.setLocalScale(.2f, 2f, .2f); 
	   			tempRockNode.yaw(Degreef.createFrom(0));
	   			smallCounter++;
	   			
	   		}
	   		else if(mediumCounter <= 13 && smallCounter + mediumCounter + largeCounter != 50) {
	   			
	   			tempRockNode.setLocalScale( .2f, 6f, .2f);
	   			//tempRockNode.yaw(Degreef.createFrom(50));

	   			mediumCounter++;
	   			
	   		}
	   		else if(smallCounter + mediumCounter + largeCounter != 50){
	   			
	   			tempRockNode.setLocalScale(.2f, 8f, .2f);
	   			largeCounter++;
	   			
	   		}
	   		
	   		tempRockNode.setLocalPosition(randomNumber.nextInt(100)-50, -1f, randomNumber.nextInt(100)-50);			//Set random position
	   		
	    	TextureManager texRockTemp = eng.getTextureManager();
	    	Texture moonRockTemp = texRockTemp.getAssetByPath("moon.jpeg");
	        RenderSystem rsd = sm.getRenderSystem();
	        TextureState stated =  (TextureState) rsd.createRenderState(RenderState.Type.TEXTURE);
	        stated.setTexture(moonRockTemp);
	        rockEntity.setRenderState(stated);
	        
	   		//Filling the respective arrays
	   		rockObjectList.add(tempRockObjectNode);
	   		rockNodeList.add(tempRockNode);
	   		//update the vertical position of the objects acc. to the terrain ***FILL IN THE INTEGER_TYPE VALUE FOR TYPE OF OBJECT YOU WANT TO UPDATE 1:npc, 2:spaceship, 3:ore, 4:crystal, 5:rock
	   		updateObjectVerticalPosition(tempRockNode, 5);
        }
        
        //Setting a planet for the looks of the game
        
		// Planet----
		planetNode = gameWorldObjectsNode.createChildSceneNode("planetNode");
		Entity planetEntity = sm.createEntity("planetEntity", "earth.obj");
		planetEntity.setPrimitive(Primitive.TRIANGLES);

		TextureManager planetTexture = eng.getTextureManager();
		Texture redTexture = planetTexture.getAssetByPath("sun.jpg");
		RenderSystem rsPlanet = sm.getRenderSystem();
		TextureState statePlanet = (TextureState)rsPlanet.createRenderState(RenderState.Type.TEXTURE);
		statePlanet.setTexture(redTexture);
		planetEntity.setRenderState(statePlanet);
		
		SceneNode planetChildNode = planetNode.createChildSceneNode(planetEntity.getName() + "Node");
		planetChildNode.setLocalPosition(500f, 125f, -225f);
		planetChildNode.setLocalScale(80f, 80f, 80f);
		planetChildNode.attachObject(planetEntity);

		// Planet 3's rotation
		RotationController rcPlanet = new RotationController(Vector3f.createUnitVectorY(), 0.005f);
		rcPlanet.addNode(planetChildNode);
		sm.addController(rcPlanet);

		//Flag 
		flagNode = gameWorldObjectsNode.createChildSceneNode("flagNode");
		Entity flagEntity = sm.createEntity("flagEntity", "flag.obj");
        flagEntity.setPrimitive(Primitive.TRIANGLES);
        //UV is laid out differently for flags, can use same textures with different colors
        TextureManager flagTex = eng.getTextureManager();
        Texture fTex = flagTex.getAssetByPath("astroTex.png");
        RenderSystem rsFlag = sm.getRenderSystem();
        TextureState stateFlag = (TextureState)rsFlag.createRenderState(RenderState.Type.TEXTURE);
        stateFlag.setTexture(fTex);
        flagEntity.setRenderState(stateFlag);
		SceneNode flagChildNode = flagNode.createChildSceneNode(flagEntity.getName() + "Node");
		flagChildNode.setLocalPosition(0, -2f, 10f);//.6
        flagChildNode.attachObject(flagEntity);
        
        // Set up Lights----
        sm.getAmbientLight().setIntensity(new Color(.3f, .3f, .3f));
		Light plight = sm.createLight("testLamp1", Light.Type.POINT);
		plight.setAmbient(new Color(.2f, .2f, .2f));
        plight.setDiffuse(new Color(.9f, .9f, .9f));
		plight.setSpecular(new Color(.9f, 1.0f, .05f));
        plight.setRange(1.75f);
       
		SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
        plightNode.attachObject(plight);
        playerEntity.getParentSceneNode().attachObject(plight);
        
        //Turn light ON and OFF--requirement
        sm.getAmbientLight().setIntensity(new Color(0.1f, 0.1f, 0.1f));
		plight2 = sm.createLight("testLamp2", Light.Type.DIRECTIONAL);
		plight2.setAmbient(new Color(.5f, .3f, .2f));
        plight2.setDiffuse(new Color(.8f, .6f, .5f));
		plight2.setSpecular(new Color(.9f, .8f, .04f));
        plight2.setRange(5f);
        planetEntity.getParentSceneNode().attachObject(plight2);
        
        //Setting up sky box   
        Configuration conf = eng.getConfiguration();
		TextureManager tm= getEngine().getTextureManager();
		tm.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
		Texture front = tm.getAssetByPath("front.png");
		Texture back = tm.getAssetByPath("back.png");
		Texture left = tm.getAssetByPath("left.png");
		Texture right = tm.getAssetByPath("right.png");
		Texture top = tm.getAssetByPath("top.png");
		Texture bottom = tm.getAssetByPath("bot.png");
		tm.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));
		
		AffineTransform xform = new AffineTransform();        
		xform.translate(0, front.getImage().getHeight());       
		xform.scale(1d, -1d);
		
		front.transform(xform);
		back.transform(xform);
		left.transform(xform);
		right.transform(xform);
		top.transform(xform);
		bottom.transform(xform);

		SkyBox sb = sm.createSkyBox(SKYBOX_NAME);        
		sb.setTexture(front, SkyBox.Face.FRONT);        
		sb.setTexture(back, SkyBox.Face.BACK);        
		sb.setTexture(left, SkyBox.Face.LEFT);        
		sb.setTexture(right, SkyBox.Face.RIGHT);        
		sb.setTexture(top, SkyBox.Face.TOP);        
		sb.setTexture(bottom, SkyBox.Face.BOTTOM);        
		sm.setActiveSkyBox(sb);
		/*
		//Terrain
		Tessellation tessE = sm.createTessellation("tessE", 6);
		tessE.setSubdivisions(8f);
		SceneNode tessN = (SceneNode) sm.getRootSceneNode().createChildNode("TessN");
		tessN.attachObject(tessE);	
		//tessN.scale(200, 100, 200);
		tessN.scale(130, 230, 130);
		//tessN.translate(Vector3f.createFrom(-6.2f, -2.2f, 2.7f));
		//tessN.yaw(Degreef.createFrom(37.2f));
		tessN.setLocalPosition(0f, -2.2f, 0f);
		//tessN.setLocalPosition(0f, 0f, 0f);
		tessE.setHeightMap(this.getEngine(), "testTerr.png");
		//https://freestocktextures.com/texture/turquoise-blue-water,941.html
		tessE.setTexture(this.getEngine(), "water.jpg");*/
		
	
		
		//this.runScript(scriptFile);
		im = new GenericInputManager();
		String kbName = im.getKeyboardName();
		//colorAction = new ColorAction(sm);
		
		initPhysicsSystem();
    	createRagePhysicsWorld();
		setupOrbitCameras(eng,sm);
        setupInputs(sm);																								//Calling the function to setup the inputs
        initAudio(sm);

    }
	//*****end of setting up the windows, cameras, scenes, objects, textures for the game*****
	
    
    //0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Physics method for the game----------------------------------------------------------------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    
    private void initPhysicsSystem() {
		// TODO Auto-generated method stub
		String engine = "ray.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {0, -4f, 0};
		physicsEng = PhysicsEngineFactory.createPhysicsEngine(engine);
		physicsEng.initSystem();
		physicsEng.setGravity(gravity);
	}
    
    private void createRagePhysicsWorld() {
		// TODO Auto-generated method stub
		 float mass = 10.0f;
		 float massS = 0.1f;
		 float massR = 5f;
		 float massF = 0.1f;
		 float radius = 1.5f;
		 float h = 2.0f;
		 //hitboxes around the object
		 float up[] = {0f,1f,0f};
		 float ship[]= {2f, 2f, 1.5f};
		 float flag[] = {.2f, 1f, .2f};
		 float r[]= {0.2f, 8f, 0.2f};
		 double[] temptf, temptf2, temptf3; 
		 
		 //meteor
		 temptf = toDoubleArray(ball1Node.getLocalTransform().toFloatArray());
		 //ball1PhysObj = physicsEng.addSphereObject(physicsEng.nextUID(),mass, temptf, 2.0f);
		 ball1PhysObj = physicsEng.addCapsuleObject(physicsEng.nextUID(), mass, temptf, radius, h);
		 ball1PhysObj.setBounciness(.01f);
		 ball1PhysObj.applyTorque(0, -2, 0);
		 //ball1PhysObj.setFriction(1);
		 ball1Node.setPhysicsObject(ball1PhysObj);
		 
		 //ship
		 temptf2 = toDoubleArray(shipNode.getLocalTransform().toFloatArray());
		 //ball2PhysObj = physicsEng.addSphereObject(physicsEng.nextUID(),massS, temptf2, 2.0f);
		 ball2PhysObj = physicsEng.addBoxObject(physicsEng.nextUID(), massS, temptf2, ship);
		 ball2PhysObj.setBounciness(.01f);
		 shipNode.setPhysicsObject(ball2PhysObj);
		 
		 //flag
		 /*temptf3 = toDoubleArray(flagNode.getLocalTransform().toFloatArray());
		 //flagPhysObj = physicsEng.addBoxObject(physicsEng.nextUID(), massF, temptf3, flag);
		 flagPhysObj = physicsEng.addCylinderObject(physicsEng.nextUID(), massF, temptf3, flag);
		 //flagPhysObj.setBounciness(.01f);
		 flagNode.setPhysicsObject(flagPhysObj);*/
		 
		 /*
		 temptf = toDoubleArray(rockNodeList.set(maxRocks, rockNode).getLocalTransform().toFloatArray());
		 rockPhysObj = physicsEng.addBoxObject(physicsEng.nextUID(), massR, temptf2, r);
		 rockPhysObj.setBounciness(0.01f);
		 rockPhysObj.applyTorque(0, -2, 0);*/
		 
		/* temptf = toDoubleArray(((Node) rockNodeList).getLocalTransform().toFloatArray());
		 //ball1PhysObj = physicsEng.addSphereObject(physicsEng.nextUID(),mass, temptf, 2.0f);
		 rockPhysObj = physicsEng.addCapsuleObject(physicsEng.nextUID(), mass, temptf, radius, h);
		 rockPhysObj.setBounciness(.01f);
		 rockPhysObj.applyTorque(0, -2, 0);
		 //ball1PhysObj.setFriction(1);
		 ((Node) rockNodeList).setPhysicsObject(rockPhysObj);*/
		 
		 temptf = toDoubleArray(groundNode.getLocalTransform().toFloatArray());
		 gndPlaneP = physicsEng.addStaticPlaneObject(physicsEng.nextUID(),temptf, up, 0.35f);
		 gndPlaneP.setBounciness(.01f);
		 gndPlaneP.setFriction(5f);
		 groundNode.scale(130f, .01f, 130f);
		 //groundNode.setLocalPosition(0f, -7.5f, -2f);
		 groundNode.setLocalPosition(0f, -9.8f, -2f);
		 groundNode.setPhysicsObject(gndPlaneP);
		
		 
	}
    
    private double[] toDoubleArray(float[] arr) {
		// TODO Auto-generated method stub
    	
		if (arr == null) 
			 return null;
		
		int n = arr.length;
		double[] ret = new double[n];
		
		for (int i = 0; i < n; i++) 
			ret[i] = (double)arr[i];
	    
		return ret;
		
	}
    
	private float[] toFloatArray(double[] arr) {
		// TODO Auto-generated method stub
		
		if (arr == null) 
			 return null;
		
		int n = arr.length;
		float[] ret = new float[n];
		
		for (int i = 0; i < n; i++)  
			ret[i] = (float)arr[i];
	    
		return ret;
		
	}
	
	//start physics by pressing space
	public void keyPressed(KeyEvent e){
		
    	switch (e.getKeyCode()){   
    	
    		case KeyEvent.VK_SPACE:System.out.println("Starting Physics!");
    			running = true;
    			break;
    		case KeyEvent.VK_L:System.out.println("Toggle the SUN !!!");
				plight2.setVisible(!plight2.isVisible());
				break;
    			
    	} 
    	
    	super.keyPressed(e);
    	
    }
	
    //**********Physics methods END**********
	
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Input handling for gamepad and keyboard----------------------------------------------------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

    //Function to get the type of controller for the game
	private void getInput() {
		// TODO Auto-generated method stub
    	ArrayList<Controller> controllers = im.getControllers();						//Get the list of all the input devices available
    	
        //Error checking to check if the controllers are connected or not (ensuring the game does not crash)
        for (Controller c : controllers) 
        	inputName = c.getName();
        
	}

	//Function to setup inputs for various actions
    protected void setupInputs(SceneManager sm){
    	
    	ArrayList<Controller> controllers = im.getControllers();						//Get the list of all the input devices available
    	
    	//Initialization action gamepad
    	//"MoveForward-sAction.java " is not used
    	moveForwardAction = new MoveForwardAction(playerNode, protClient, this, true);  
    	moveBackwardAction = new MoveBackwardAction(playerNode, this);						
        moveLeftAction = new MoveLeftAction(playerNode);								
        moveRightAction = new MoveRightAction(playerNode);	
        rotateRightA = new RotateRightAction(playerNode, protClient, camera);
        rotateLeftA = new RotateLeftAction(playerNode, protClient, camera);
        rotateAction = new RotateAction(this);
        moveDirectionAction = new MoveDirectionAction(playerNode, this);
        moveUpDownAction = new MoveUpDownAction(playerNode, this);
        rotatePlayerAction = new RotatePlayerAction(playerNode);

        //Error checking to check if the controllers are connected or not (ensuring the game does not crash)
        for (Controller c : controllers) {
        	
        	//If the controller type is keyboard, then use the keyboard controls, otherwise use the gamepad controls
            if (c.getType() == Controller.Type.KEYBOARD)
                keyboardControls(c);													//Call the keyboard Control function to handle the keyboard inputs
            else if (c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK)
                gamepadControls(c);														//Call the gamepad input to control the XB1 inputs
            
        }
        
    }
    
    //Function to handle the gamepad controlls
    void gamepadControls(Controller gpName) {
    	
    	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.POV, moveCameraAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);  
    	im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.X, rotatePlayerAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);  
    	im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.Y, moveUpDownAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN); 
    	im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.RX, rotateAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.RY, rotateAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);	

    }

    //Function to handle the keyboard controls
    void keyboardControls(Controller kbName) {
   
        //im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.D, moveLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        //im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.A, moveRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.W, moveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        //im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.S, moveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.A, rotateLeftA, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.D, rotateRightA, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        
    }
    
	//*****end of input handling*****
    
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Networking part of the game----------------------------------------------------------------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    
    //Function to setup networking
    private void setupNetworking() {
    	
    	gameObjectsToRemove = new Vector<UUID>();
    	isClientConnected = false;
    	
    	try {   
    		protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
    	}   
    	catch (UnknownHostException e) { 
    		e.printStackTrace();
    	}   
    	catch (IOException e) { 
    		e.printStackTrace();
    	} 
    	if (protClient == null){   
    		System.out.println("missing protocol host"); 
    	} 
    	else{ 
    		// ask client protocol to send initial join message/ /to server, with a unique identifier for this 
    		protClient.sendJoinMessage();
    	}
    	
    }
    
    //Function to process networking
    protected void processNetworking(float elapsTime) {
    	
    	if(protClient != null) { 
    		protClient.processPackets();
    	}
    	//Remove ghost avatars for players who have left the game
    	Iterator<UUID> it = gameObjectsToRemove.iterator();
    	
    	while(it.hasNext()) { 
    		sm.destroySceneNode(it.next().toString());
    	}
    	gameObjectsToRemove.clear();
    	
    }

    //Function to check if the client is connected or not
	public void setIsConnected(boolean b) {
		this.isClientConnected = b;
		
	}
	//Function to add the ghost avatar of the other player
	public void addGhostAvatarToGameWorldnew(GhostAvatar avatar, Vector3 ghostPosition) throws IOException{
		
		if(avatar!=null) {
			
			SkeletalEntity ghostSE = getEngine().getSceneManager().createSkeletalEntity("ghost", "astroRig.rkm", "astro.rks");
			ghostSE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = getEngine().getSceneManager().getRootSceneNode().createChildSceneNode(avatar.getID().toString());
			//EDDIT
	        //ghostN = avatar.getGhostN().createChildSceneNode(ghostSE.getName() + "Node");
	        
			ghostN.attachObject(ghostSE);
			ghostN.setLocalPosition(ghostPosition);
			ghostN.scale(.06f, .06f, .06f);
			avatar.setNode(ghostN);
			avatar.setSE(ghostSE);
			avatar.setPosition(ghostPosition.x(), ghostPosition.y(), ghostPosition.z());
			System.out.println("ghost added " + ghostPosition);
			
			TextureManager tmd1 = getEngine().getTextureManager();
	        Texture assetd1 = tmd1.getAssetByPath("astroTex1.png");
	        RenderSystem rsd1 = getEngine().getSceneManager().getRenderSystem();
	        TextureState stated1 =  (TextureState) rsd1.createRenderState(RenderState.Type.TEXTURE);
	        stated1.setTexture(assetd1);
	        ghostSE.setRenderState(stated1);
	        ghostSE.loadAnimation("walk", "walk2.rka");
	        ghostCheckN = true;
	        //EDIT Vertical position

		}
		
	}
	
	public void addGhostAvatarToGameWorldold(GhostAvatar avatar, Vector3 ghostPosition) throws IOException{
		if(avatar!=null) {
			SkeletalEntity ghostSE = getEngine().getSceneManager().createSkeletalEntity("ghost", "astroRig.rkm", "astro.rks");
			//ghostE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = getEngine().getSceneManager().getRootSceneNode().createChildSceneNode(avatar.getID().toString());
			//EDDIT
	        //ghostN = avatar.getGhostN().createChildSceneNode(ghostSE.getName() + "Node");

			ghostN.attachObject(ghostSE);
			ghostN.setLocalPosition(ghostPosition);
			ghostN.scale(.06f, .06f, .06f);
			avatar.setNode(ghostN);
			avatar.setSE(ghostSE);
			avatar.setPosition(ghostPosition.x(), ghostPosition.y(), ghostPosition.z());
			System.out.println("ghost added " + ghostPosition);
			
			TextureManager tmd1 = getEngine().getTextureManager();
	        Texture assetd1 = tmd1.getAssetByPath("astroTex1.png");
	        RenderSystem rsd1 = getEngine().getSceneManager().getRenderSystem();
	        TextureState stated1 =  (TextureState) rsd1.createRenderState(RenderState.Type.TEXTURE);
	        stated1.setTexture(assetd1);
	        ghostSE.setRenderState(stated1);
	        ghostSE.loadAnimation("walk", "walk2.rka");
	        ghostCheckN = true;
	        
		}
		
	}
	
	//Function to remove Ghost avatar
	public void removeGhostAvatarFromGameWorld(GhostAvatar avatar) {
		if(avatar!=null)
			gameObjectsToRemove.add((UUID)avatar.getID());
		
	}
	
	//Function to communicate
	private class SendCloseConnectionPacketAction extends AbstractInputAction{
		
		// for leaving the game... need to attach to an input device
    	@Override
    	public void performAction(float arg0, net.java.games.input.Event arg1){
    	 
    		if(protClient != null && isClientConnected == true) 
    			protClient.sendByeMessage();
    		
    	} 
    	
	}
	
	//*****end of networking functionalities*****
	
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Getter and setter functions----------------------------------------------------------------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	
	//Function to return the camera Elevation
	public void setCameraElevationAngle(float newAngle) {
		
		this.playerController.setCameraElevationAngle(newAngle);
		
	}

	public float getCameraElevationAngle() {
		
		return this.playerController.getCameraElevationAngle();
		
	}

	public void setRadius(float r) {
		
		this.playerController.setRadius(r);
		
	}

	public float getRadius() {
		
		return this.playerController.getRadius();
		
	}

	public void setCameraAzimuthAngle(float newAngle) {
		
		this.playerController.setAzimuth(newAngle);
		
	}

	public float getCameraAzimuthAngle() {
		
		return this.playerController.getAzimuth();
		
	}
	 
    //Function to return the position of the player
    
    
	//*****end of getter & setter methods*****
    
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Update function for the game---------------------------------------------------------------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    
    //Function to keep updating all the gameWorld variables and states after each iteration
    public void setWalkTrue(boolean animW)
    {
    	walkB = animW;
    }
    public void setWalkFalse()
    {
    	walkB = false;
    }
    public void setIdleTrue(boolean animI)
    {
    	idleB = animI;
    }
    public void setIdleFalse()
    {
    	idleB = false;
    }
    
    private void doTheWalk() {
    	
    	SkeletalEntity playerEntity = (SkeletalEntity) getEngine().getSceneManager().getEntity("myPlayer");
    	playerEntity.stopAnimation();
    	playerEntity.playAnimation("walk", 0.75f, LOOP, 0);
    	
    }
    private void doTheWalk2() {
    	
    	SkeletalEntity ghostSE = (SkeletalEntity) getEngine().getSceneManager().getEntity("ghost");
    	//ghostSE.stopAnimation();
    	ghostSE.playAnimation("walk", 0.75f, LOOP, 0);
    	
    }
    private void Idle() {
    	
    	SkeletalEntity playerEntity = (SkeletalEntity) getEngine().getSceneManager().getEntity("myPlayer");
    	playerEntity.stopAnimation();
    	playerEntity.playAnimation("idle", .9f, LOOP, 0);
    	

    }
    
    private void doTheStop() {
    	
    	SkeletalEntity playerEntity = (SkeletalEntity) getEngine().getSceneManager().getEntity("myPlayer");
    	playerEntity.stopAnimation();
    	
    }
    
    @Override
    protected void update(Engine engine) {
    	
		// build and set HUD
		rs = (GL4RenderSystem) engine.getRenderSystem();
		elapsTime += engine.getElapsedTimeMillis();
		elapsTimeSec = Math.round(elapsTime/1000.0f);
		elapsTimeStr = Integer.toString(elapsTimeSec);
		im.update(elapsTime);	
		playerController.updateCameraPosition();
		dispStr = "Player time = "+elapsTimeStr+"  Ores Collected = "+ oresCount + "/" + maxOre + "            " + winner;

		rs.setHUD(dispStr, 15, 25);

		//physics
		if(running) {
			
			Matrix4 mat;
			physicsEng.update(elapsTime);
			
			for(SceneNode s: engine.getSceneManager().getSceneNodes()){
				
				if(s.getPhysicsObject()!=null) {
					
					mat = Matrix4f.createFrom(toFloatArray(s.getPhysicsObject().getTransform()));
					s.setLocalPosition(mat.value(0, 3), mat.value(1, 3), mat.value(2, 3));
					
				}
				
			}
			
		}
		
		//animations----
		if(ghostCheckN) {
    		SkeletalEntity ghostSE = (SkeletalEntity) engine.getSceneManager().getEntity("ghost");
    		ghostSE.update();
    		if(walkG2) {
    			setWalkFalse2();
            }
            else {
            	doTheWalk2();
            }
    	}
		
		SkeletalEntity playerEntity = (SkeletalEntity) engine.getSceneManager().getEntity("myPlayer");
    	playerEntity.update();
    	SkeletalEntity npcEntity = (SkeletalEntity) engine.getSceneManager().getEntity("npc");
    	npcEntity.update();
		if(walkB) 
	        setWalkFalse();
	    else 
	    	doTheWalk();
		
    	
        
        
		//Updating the sound variables with each gameEngine cycle
		SceneManager sm = engine.getSceneManager();
		SceneNode playerNode = sm.getSceneNode("myPlayerNode"), shipNode = sm.getSceneNode("myShipNode");		
		//stepSound.setLocation(tempNode.getWorldPosition());
		//bgSound.setLocation(playerNode.getWorldPosition());
		sparkSound.setLocation(shipNode.getWorldPosition());
		setEarParameters(sm);
		
		//Update the input manager with elapsed time
		im.update(elapsTime);	
		processNetworking(elapsTime);

		//Printing out the position of the player in the world to the console
		//Debug terrain
		//System.out.println(playerNode.getWorldPosition().toString());
		
		
		//collision
		SceneNode player = getEngine().getSceneManager().getSceneNode("myPlayerNode");
		SceneNode ship = getEngine().getSceneManager().getSceneNode("shipNode");
		for(int i =0; i<= oreNodeList.size()-1; i++)
		{
			SceneNode target = oreNodeList.get(i);
			if(colDetection(player, target)< 1f)
			{
				oreNodeList.remove(i);
				
				player.attachChild(target);
				oresCount++;
				//System.out.println("cyrtal collected" + oresCount);
			}
		}
		if(oreNodeList.size() < 1 && colDetection(player, ship)<30)
		{
			//winner = "All ores collected head back to the ship!";
			winner = "CAREFUL! METEOR!";
			running = true;
			//turn on physics
			//System.out.println("Winner");
		}
		
	}
    //collision detection
    private float colDetection(SceneNode s, SceneNode d){
		float sX = s.getLocalPosition().x(),
				  sY = s.getLocalPosition().y(),
			      sZ = s.getLocalPosition().z();
		float mX = d.getLocalPosition().x(),
			      mY = d.getLocalPosition().y(),
				  mZ = d.getLocalPosition().z();
			
		double a, b, c;
		a = Math.pow(sX-mX, 2);
		b = Math.pow(sY-mY, 2);
		c = Math.pow(sZ-mZ, 2);
			
		double result;
		result = Math.sqrt((double)a + b + c);
		return (float)result;
		
	}

    
    public void updateObjectVerticalPosition(SceneNode tempNode, int type) {
																																											  
		
		//int types  ==>  1:npc, 2:spaceship, 3:ore, 4:crystal, 5:rock
		float delta = 0.5f;
		if(type == 1)
			delta = 5f;//1.5f
		else if(type == 2)
			delta = 0.5f;
		else if(type ==3)
			delta = 0.8f;//.5f
		else if(type == 4)
			delta = 0.16f;
		else if(type == 5)
			delta = 0.0f;
		
		//Getting and setting the info. variables
		SceneNode objectNode = tempNode;
		SceneNode tessNode = this.getEngine().getSceneManager().getSceneNode("TessN");
		Tessellation tessEntity = ((Tessellation)tessNode.getAttachedObject("tessE"));
		Vector3 objectPosition = objectNode.getWorldPosition();
		Vector3 localObjectPosition = objectNode.getLocalPosition();
		Vector3 newObjectPosition = Vector3f.createFrom(localObjectPosition.x(), tessEntity.getWorldHeight(objectPosition.x(), objectPosition.z()) + delta, localObjectPosition.z());
		objectNode.setLocalPosition(newObjectPosition);																								//Updating the object location location
		
	}
    
    //Function to update the player height according to the terrain----
	public void updateVerticalPosition() {
		
		//Getting and setting the info. variables

		SceneNode playerNode = this.getEngine().getSceneManager().getSceneNode("myPlayerNode");
		
		SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("TessN");
		Tessellation tessE = ((Tessellation) tessN.getAttachedObject("tessE"));
		Vector3 worldAvatarPosition = playerNode.getWorldPosition();
		Vector3 localAvatarPosition = playerNode.getLocalPosition();
		//Vector3 newPlayerPosition = Vector3f.createFrom(localAvatarPosition.x(), tessE.getWorldHeight(worldAvatarPosition.x(), worldAvatarPosition.z()), localAvatarPosition.z());
		
		float terrHeight = tessE.getWorldHeight(worldAvatarPosition.x()+.1f, worldAvatarPosition.z()+.1f);
		Vector3 newAvatarPosition = (Vector3)Vector3f.createFrom(localAvatarPosition.x(), terrHeight+.5f, localAvatarPosition.z());
		playerNode.setLocalPosition(newAvatarPosition);
		//System.out.println("Tessellation height at player position:" + tessEntity.getWorldHeight(playerPosition.x(), playerPosition.z()));
		
		//playerNode.setLocalPosition(newPlayerPosition);																								//Updating the player location
		
	}
    
	//*****end of update function*****
        
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Scripting Functionalities for the game-----------------------------------------------------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
    
    //Function to run the script file
    private void runScript(File scriptFile) {
    	
    	 try{ 
    		 FileReader fileReader = new FileReader(scriptFile);      
    		 jsEngine.eval(fileReader);      
    		 fileReader.close();   
    	 }
    	 catch (FileNotFoundException e1){ 
    		 System.out.println(scriptFile + " not found " + e1); 
    	 }
    	 catch (IOException e2){ 
    		 System.out.println(scriptFile + " not found " + e2); 
    	 }
    	 catch (ScriptException e3){ 
    		 System.out.println(scriptFile + " not found " + e3); 
    	 }
    	 catch (NullPointerException e4){ 
    		 System.out.println(scriptFile + " not found " + e4); 
    	 }
    	 
    }
    
	//*****end of scripting Functionalities*****

	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	//Sound functionalities for the game---------------------------------------------------------------------------------------------------------------------------------------
	//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

    //Function to initialize the audio
    public void initAudio(SceneManager sm) {
    	
    	AudioResource  resource1, resource2, resource3;
    	audioManager = AudioManagerFactory.createAudioManager("ray.audio.joal.JOALAudioManager");
    	
    	if(!audioManager.initialize()) {

    		System.out.println("Audio manager failed to initialize");
    		return;
    				
    	}
    	
    	//Setting the resources with .wav files for the game
    	//resource1 =  audioManager.createAudioResource("Sounds/step.wav", AudioResourceType.AUDIO_SAMPLE);
    	resource2 = audioManager.createAudioResource("Sounds/Background.wav", AudioResourceType.AUDIO_SAMPLE);
    	resource3 = audioManager.createAudioResource("Sounds/sparks.wav", AudioResourceType.AUDIO_SAMPLE);

    	
    	//Setting attributes for the sound
		/*
		 * stepSound = new Sound(resource1, SoundType.SOUND_EFFECT, 100, true);
		 * stepSound.initialize(audioManager); 
		 * stepSound.setMaxDistance(10.0f);
		 * stepSound.setMinDistance(0.5f); 
		 * stepSound.setRollOff(5.0f);
		 */
    	
    	bgSound = new Sound(resource2, SoundType.SOUND_MUSIC, 100, true);
    	bgSound.initialize(audioManager);
    	
		sparkSound = new Sound(resource3, SoundType.SOUND_EFFECT, 100, true);
		sparkSound.initialize(audioManager); 
		sparkSound.setMaxDistance(2.0f);
		sparkSound.setMinDistance(0.5f); 
		sparkSound.setRollOff(10.0f);
        	
    	//Attaching the sounds to the space ship
    	SceneNode spaceShip = sm.getSceneNode("myShipNode");
    	sparkSound.setLocation(spaceShip.getWorldPosition());
    	
    	//Setting the ear parameters for the player
    	setEarParameters(sm);
    	
    	//Playing the sounds
    	//stepSound.play();
    	bgSound.play();
    	sparkSound.play();
    	
    }
    
    //Function to set the ear parameters for the player
    public void setEarParameters(SceneManager sm) {
    	
    	SceneNode playerNode = sm.getSceneNode("myPlayerNode");
    	Vector3 avDir = playerNode.getWorldForwardAxis();
    	audioManager.getEar().setLocation(playerNode.getWorldPosition());
    	audioManager.getEar().setOrientation(avDir, Vector3f.createFrom(0,1,0));
    	
    }

	public Matrix3 getPlayerOrientation() {
		// TODO Auto-generated method stub
		return playerNode.getLocalRotation();
	}
	public Vector3 getPlayerPosition() {
    	
    	//SceneNode playerNode = sm.getSceneNode("myPlayerNode");
    	return playerNode.getLocalPosition();
    	
    }
	public void moveNodeF(UUID ghostID, Vector3 ghostPosition) {
		// TODO Auto-generated method stub
		//SceneNode ghostN = this.getEngine().getSceneManager().getSceneNode("ghostNode");

		Vector<GhostAvatar> ghostAvatars = protClient.getCollection();
		ghostAvatars.get(0).getGhostN().moveForward(.05f);
		setWalkTrue2();
		/*if(ghostCheckN) {
			updateVertGhostNew(ghostID, ghostPosition);
		}
		updateVertGhostOld(ghostID, ghostPosition);*/
		//SceneNode ghostN = ghostAvatars.get(0).getGhostN();
		
		
	}
	public void setWalkTrue2() {
		walkG2 = true;
	}
	public void setWalkFalse2() {
		walkG2 = false;
	}
	public void updateVertGhostNew(UUID ghostID, Vector3 ghostPosition) {
		// TODO Auto-generated method stub  
		//SceneNode ghostN = this.getEngine().getSceneManager().getSceneNode("ghostNode");
		SceneNode ghostN = getEngine().getSceneManager().getRootSceneNode().createChildSceneNode(ghostID.toString());

		SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("TessN");
		Tessellation tessE = ((Tessellation) tessN.getAttachedObject("tessE"));
		
		Vector3 worldAvatarPositionGhost = ghostN.getWorldPosition();
		Vector3 localAvatarPositionGhost = ghostN.getLocalPosition();
		
		float terrHeight = tessE.getWorldHeight(worldAvatarPositionGhost.x()+.1f, worldAvatarPositionGhost.z()+.1f);
		
		Vector3 newAvatarPositionGhost = (Vector3)Vector3f.createFrom(localAvatarPositionGhost.x(), terrHeight+.5f, localAvatarPositionGhost.z());
		ghostN.setLocalPosition(newAvatarPositionGhost);
        protClient.sendMoveMessage(ghostN.getWorldPosition(), "Vert");

		
	}

	private void updateVertGhostOld(UUID ghostID, Vector3 ghostPosition) {
		// TODO Auto-generated method stub
		//SceneNode ghostN = this.getEngine().getSceneManager().getSceneNode("ghostNode");
		SceneNode ghostN = getEngine().getSceneManager().getRootSceneNode().createChildSceneNode(ghostID.toString());

		SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("TessN");
		Tessellation tessE = ((Tessellation) tessN.getAttachedObject("tessE"));
		
		Vector3 worldAvatarPositionGhost = ghostN.getWorldPosition();
		Vector3 localAvatarPositionGhost = ghostN.getLocalPosition();
		
		float terrHeight = tessE.getWorldHeight(worldAvatarPositionGhost.x()+.1f, worldAvatarPositionGhost.z()+.1f);
		
		Vector3 newAvatarPositionGhost = (Vector3)Vector3f.createFrom(localAvatarPositionGhost.x(), terrHeight+.5f, localAvatarPositionGhost.z());
		ghostN.setLocalPosition(newAvatarPositionGhost);
        protClient.sendMoveMessage(ghostN.getWorldPosition(), "Vert");

	}
    public void addGhostNPCGameWorld(GhostNPC npc, int id) throws IOException{
    	if(npc!=null) {
    		Entity npcE = getEngine().getSceneManager().createEntity("npc" +id, "astro.obj");
    		//npcE.setPrimitive(Primitive.TRIANGLES);
    		SceneNode npcN = getEngine().getSceneManager().getRootSceneNode().createChildSceneNode("npcNode1");
    		npcN.attachObject(npcE);
    		
    		npcN.setLocalPosition(2.5f, -0.3f, 0);
    		
    		TextureManager tmd1 = getEngine().getTextureManager();
	        Texture assetd1 = tmd1.getAssetByPath("astroTex2.png");
	        RenderSystem rsd1 = getEngine().getSceneManager().getRenderSystem();
	        TextureState stated1 =  (TextureState) rsd1.createRenderState(RenderState.Type.TEXTURE);
	        stated1.setTexture(assetd1);
	        npcE.setRenderState(stated1);
    		
    		System.out.println("npc added");
    		
    	}
    }
	//*****end of sound Functionalities*****

	public void rotateLN(UUID ghostID, Vector3 ghostPosition) {
		// TODO Auto-generated method stub
		Vector<GhostAvatar> ghostAvatars = protClient.getCollection();
		Angle rotAmt1 = Degreef.createFrom(5f);
		ghostAvatars.get(0).getGhostN().yaw(rotAmt1);
	}

	public void rotateRN(UUID ghostID, Vector3 ghostPosition) {
		// TODO Auto-generated method stub
		Vector<GhostAvatar> ghostAvatars = protClient.getCollection();
		Angle rotAmt1 = Degreef.createFrom(-5f);
		ghostAvatars.get(0).getGhostN().yaw(rotAmt1);
	}

	public void getSmall(NPC npc) {
		if(npc!=null)
		{
			SceneNode npcN = getEngine().getSceneManager().getSceneNode("npcNode1");
			npcN.setLocalScale(0.5f, 0.5f, 0.5f);
		}
	}

	public void getBig(NPC npc) {
		if(npc!=null)
		{
			SceneNode npcN = getEngine().getSceneManager().getSceneNode("npcNode1");
			npcN.setLocalScale(3f, 3f, 3f);
		}
	}

	public double getSize(NPC npc) {
		if(npc!=null)
		{
			SceneNode npcN = getEngine().getSceneManager().getSceneNode("npcNode1");
			double size = npcN.getLocalScale().x();
			return size;
		}else
			return 0;
	}
	// Script
	private void executeScript(ScriptEngine engine, String scriptFile2)
	{
		try
		{
			FileReader fileReader = new FileReader(scriptFile2);
			engine.eval(fileReader);
			fileReader.close();
		}
		catch(FileNotFoundException e1){
			System.out.println(scriptFile2 + "not found" +e1);
		}
		catch(IOException e2){
			System.out.println("IO problem with " + scriptFile2 + "not found" +e2);
		}
		catch(ScriptException e3){
			System.out.println("ScriptException in " + scriptFile2 + "not found" +e3);
		}
		catch(NullPointerException e4){
			System.out.println("Null pointer exception " +scriptFile2 + "not found" +e4);
		}
	}
}

