package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.system.AppSettings;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VarokaaHeikkoaJaata extends SimpleApplication implements ActionListener, PhysicsCollisionListener {

    private BulletAppState bulletAppState;
    private CharacterControl playerControl;
    private RigidBodyControl landscapeControl;
    private Node playerNode;
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private Vector3f viewDirection = new Vector3f(0, 0, 0);
    public static final Set<String> ACTIVE = new HashSet<String>();
    private Spatial playerSpatial, landscapeSpatial;
    private AudioNode aani;
    private AudioNode soiva = null;
    private float wobble;
    private static final float WATER_LEVEL = -5.0f;
    private static final String PLAYER = "Nalle";
    private static final String HOUSE = "Talo";
    private static final String UNDERWATER = "Vesihiisi";
    private static final String BIRD = "Tirppa";
    private static final String ICE = "Jaa";
    private Ice ice;
    private boolean peliLoppu;
    private Node houseNode;
    public Nifty nifty;
    private Node birdNode;
    
    public static void main(String[] args) {
        VarokaaHeikkoaJaata app = new VarokaaHeikkoaJaata();
        AppSettings s = new AppSettings(true);
        s.setFrameRate(100);
        s.setSettingsDialogImage("Images/nalleavannossa.jpg");
        app.setPauseOnLostFocus(true);
        app.setSettings(s);
        app.start();
    }


    @Override
    public void simpleInitApp() {

        // show instructions with nifty
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);

        nifty = niftyDisplay.getNifty();
        nifty.fromXml("Interface/Screens/screen.xml", "gameScreen");

        guiViewPort.addProcessor(niftyDisplay);


        // activate physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        //DEBUG
     //   getPhysicsSpace().enableDebug(assetManager);

        //environment setup
        setupEnvironment();

        //player setup
        setupPlayer();


        startGame();
    }

    private void soitaAani(Aani soitettava, boolean looping) {
        aani = new AudioNode(assetManager, soitettava.annaAani(), false);
        if (soiva != null) {
            lopetaAani();
        }
        aani.setLooping(looping);
        aani.setPositional(true);
        aani.setLocalTranslation(Vector3f.ZERO.clone());
        aani.setVolume(3);
        rootNode.attachChild(aani);
        aani.play();
        soiva = aani;
    }

    private void lopetaAani() {
        soiva.stop();
        soiva = null;
    }

    public void startGame() {

        //cam
        setupCam();

        //registering inputs for target's movement
        registerInput();
        soitaAani(Aani.KAVELY, true);

        //Kuuntelee collisioneita
        getPhysicsSpace().addCollisionListener(this);

        peliLoppu = false;

        //    nifty.removeScreen(nifty.getCurrentScreen().getScreenId());
        //    nifty.gotoScreen(nextScreen);
    }
    
    private void restartGame() {
        playerControl.setPhysicsLocation(new Vector3f(0f, 0f, 0f));
        this.showAlert("");
        this.soitaAani(Aani.KAVELY, true);
        this.peliLoppu = false;
    }

    public void quitGame() {
        System.out.println("FUU FUU");
        this.stop();
    }

    private void showAlert(String text) {
        Element textElement = nifty.getCurrentScreen().findElementByName("alert_text");
        textElement.getRenderer(TextRenderer.class).setText(text);
    }

    private static final class Move {

        public static final String LEFT = "LEFT", RIGHT = "RIGHT",
                FORWARD = "FORWARD", BACKWARD = "BACKWARD", JUMP = "JUMP",
                RESTART = "SHOOT";
    }

    public void registerInput() {
        inputManager.addMapping(Move.LEFT,
                new KeyTrigger(KeyInput.KEY_A),
                new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(Move.RIGHT,
                new KeyTrigger(KeyInput.KEY_D),
                new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(Move.FORWARD,
                new KeyTrigger(KeyInput.KEY_W),
                new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(Move.BACKWARD,
                new KeyTrigger(KeyInput.KEY_S),
                new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping(Move.JUMP,
                new KeyTrigger(KeyInput.KEY_SPACE),
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(Move.RESTART,
                new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, Move.LEFT, Move.RIGHT);
        inputManager.addListener(this, Move.FORWARD, Move.BACKWARD);
        inputManager.addListener(this, Move.JUMP, Move.RESTART);
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals(Move.LEFT)) {
            if (value) {
                ACTIVE.add(Move.LEFT);
            } else {
                ACTIVE.remove(Move.LEFT);
            }
        } else if (binding.equals(Move.RIGHT)) {
            if (value) {
                ACTIVE.add(Move.RIGHT);
            } else {
                ACTIVE.remove(Move.RIGHT);
            }
        } else if (binding.equals(Move.FORWARD)) {
            if (value) {
                ACTIVE.add(Move.FORWARD);
            } else {
                ACTIVE.remove(Move.FORWARD);
            }
        } else if (binding.equals(Move.BACKWARD)) {
            if (value) {
                ACTIVE.add(Move.BACKWARD);
            } else {
                ACTIVE.remove(Move.BACKWARD);
            }
        } else if (binding.equals(Move.JUMP) && !peliLoppu) {
            playerControl.jump();
        } else if (binding.equals(Move.RESTART)) {
            this.restartGame();
        }
    }

    private void setupCam() {
        CameraNode camNode = new CameraNode("CamNode", cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 3, -8));
        camNode.lookAt(playerSpatial.getLocalTranslation().add(0f, 2f, 0f), Vector3f.UNIT_Y);
        playerNode.attachChild(camNode);
        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(0f);
        flyCam.setRotationSpeed(2.0f);
        flyCam.setDragToRotate(false);
        
    }

    /**
     * Sets up the sun, water etc.
     */
    private void setupEnvironment() {

        //sky
        Texture sky = assetManager.loadTexture("Textures/taivas/sininen.png");
        rootNode.attachChild(SkyFactory.createSky(assetManager, sky, sky, sky, sky, sky, sky));

        //SCENE
        //heightmap
        AbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/heightmap/lake.jpg");
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();


        landscapeSpatial = new TerrainQuad("Terrain", 65, 513, heightmap.getHeightMap());

        Material landscapeMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        landscapeMaterial.setTexture("ColorMap", assetManager.loadTexture("Textures/snow_2.png"));
        landscapeSpatial.setMaterial(landscapeMaterial);
        landscapeSpatial.setLocalTranslation(0, -62, 110);
        landscapeSpatial.setLocalScale(0.8f, 0.5f, 0.8f);

        TerrainLodControl control = new TerrainLodControl((Terrain) landscapeSpatial, getCamera());
        landscapeSpatial.addControl(control);


        CollisionShape terrainShape =
                CollisionShapeFactory.createMeshShape((Node) landscapeSpatial);
        landscapeControl = new RigidBodyControl(terrainShape, 0);
        landscapeSpatial.addControl(landscapeControl);


        landscapeSpatial.setShadowMode(ShadowMode.Receive);

        rootNode.attachChild(landscapeSpatial);
        getPhysicsSpace().add(landscapeControl);

        //sun
        DirectionalLight sun = new DirectionalLight();
        final Vector3f lightDir = new Vector3f(0.36f, 0.50f, 0.80f);
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(1.1f));
        rootNode.addLight(sun);


        //water
        ColorRGBA waterColor = new ColorRGBA(0.23f, 0.27f, 0.28f, 1.0f);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        final WaterFilter water = new WaterFilter(rootNode, lightDir);
        water.setWaterHeight(WATER_LEVEL);
        water.setUseFoam(true);
        water.setUseRipples(true);
        water.setDeepWaterColor(waterColor.mult(0.3f));
        water.setWaterColor(waterColor);
        water.setWaterTransparency(0.3f);
        water.setMaxAmplitude(0.1f);
        water.setWaveScale(0.03f);
        water.setSpeed(0.4f);
        water.setShoreHardness(1.0f);
        water.setRefractionConstant(0.7f);
        water.setShininess(0.1f);
        water.setSunScale(0.9f);
        water.setColorExtinction(new Vector3f(10.0f, 20.0f, 30.0f));
        fpp.addFilter(water);
        viewPort.addProcessor(fpp);

        //ICE
        ice = new Ice(ICE, assetManager);
        rootNode.attachChild(ice);
        ice.setLocation(new Vector3f(0f, WATER_LEVEL + 0.3f, 0f));
        getPhysicsSpace().add(ice);

        //house
        houseNode = new Node(HOUSE);
        Spatial houseSpatial = assetManager.loadModel("Models/talo1/talo1.mesh.j3o");
        houseSpatial.scale(7.0f);

        houseNode.rotate(0f, FastMath.PI, 0f);

        CollisionShape houseShape =
                CollisionShapeFactory.createMeshShape((Node) houseSpatial);
        RigidBodyControl houseControl = new RigidBodyControl(houseShape, 0);
        houseNode.addControl(houseControl);
        getPhysicsSpace().add(houseControl);
        houseControl.setPhysicsLocation(new Vector3f(16.0f, 1.0f, 250.0f));
        houseNode.attachChild(houseSpatial);
        
        //bird
        birdNode = new Node(BIRD);
        Spatial birdSpatial = assetManager.loadModel("Models/lintu/laatikkolintu.mesh.j3o");
        birdSpatial.scale(1.0f);

        birdNode.rotate(0f, FastMath.PI, 0f);

        CollisionShape birdShape =
                CollisionShapeFactory.createBoxShape(birdSpatial);
        RigidBodyControl birdControl = new RigidBodyControl(birdShape, 2.0f);
        birdSpatial.setLocalTranslation(0f, -1f, 0f);
        birdNode.addControl(birdControl);
        getPhysicsSpace().add(birdControl);
        birdControl.setPhysicsLocation(new Vector3f(40.0f, 2.0f, 220.0f));
        birdNode.attachChild(birdSpatial);
        
        

        rootNode.attachChild(birdNode);

        rootNode.attachChild(houseNode);
        
        
    }

    private void setupPlayer() {
        // Add a physics character to the world
        playerControl = new CharacterControl(new CapsuleCollisionShape(0.8f, 1.8f), .1f);
        playerControl.setPhysicsLocation(new Vector3f(-3, 0, 0));
        playerNode = new Node(PLAYER);
        playerSpatial = assetManager.loadModel("Models/karhu/karhu.mesh.j3o");
        playerSpatial.scale(8.0f);
        playerNode.addControl(playerControl);
        getPhysicsSpace().add(playerControl);
        rootNode.attachChild(playerNode);

        playerSpatial.setLocalTranslation(0.0f, -1.7f, 0.0f);
        playerNode.attachChild(playerSpatial);

        playerSpatial.setShadowMode(ShadowMode.CastAndReceive);
        playerControl.setJumpSpeed(20f);
        playerControl.setFallSpeed(20f);
        playerControl.setGravity(60f);
        
        playerControl.setViewDirection(playerControl.getViewDirection().clone().negate());
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    @Override
    public void simpleUpdate(float tpf) {
        float movement_amount = 0.3f;
        // Gets forward direction and moves it forward
        Vector3f direction = cam.getDirection().clone().multLocal(movement_amount);
        // Gets left direction and moves it to the left
        Vector3f leftDirection = cam.getLeft().clone().multLocal(movement_amount);

        direction.y = 0;
        leftDirection.y = 0;

        walkDirection.set(0, 0, 0);
        viewDirection.set(direction);

        if (!peliLoppu) {

            if (ACTIVE.contains(Move.LEFT)) {
                viewDirection.addLocal(leftDirection.mult(0.05f));
            } else if (ACTIVE.contains(Move.RIGHT)) {
                viewDirection.addLocal(leftDirection.mult(0.05f).negate());
            }
            // loppuiko?
            if (ACTIVE.contains(Move.FORWARD)) {
                walkDirection.addLocal(direction);
            } else if (ACTIVE.contains(Move.BACKWARD)) {
                walkDirection.addLocal(direction.negate());
            }

            if (ACTIVE.contains(Move.FORWARD) || ACTIVE.contains(Move.BACKWARD)) {
                wobble = (wobble + 0.1f) % FastMath.TWO_PI;
                float rotation = 0.01f * (float) FastMath.sin(wobble);
                playerSpatial.rotate(0f, 0f, rotation);
                // final Vector3f newUp = new Vector3f(rotation, 1.0f - rotation * rotation, 0f);
                //playerSpatial.rotateUpTo(newUp);
            }
        } else {
            viewDirection.addLocal(leftDirection.mult(0.07f));
        }


        playerControl.setWalkDirection(walkDirection);
        playerControl.setViewDirection(viewDirection);



        //  System.out.println(playerControl.getPhysicsLocation() + " " + wobble); //DEBUG
    }

    public void collision(PhysicsCollisionEvent event) {

        if (FastMath.nextRandomFloat() < 0.3f) {
            if (event.getNodeA().getName().equals(PLAYER)) {
                handlePlayerCollision(event.getNodeB().getName(), event);
            } else if (event.getNodeB().getName().equals(PLAYER)) {
                handlePlayerCollision(event.getNodeA().getName(), event);
            }
        }
    }

    private void handlePlayerCollision(String objectName, PhysicsCollisionEvent event) {
        if (objectName.equals(HOUSE)) {
            this.voitaPeli();
        } else if (objectName.equals(ICE)) {
            this.kaveleJaalla();
        }
    }

    private void voitaPeli() {
        if (!peliLoppu) {
            this.peliLoppui();
            this.soitaAani(Aani.GANGNAM, false);
            this.showAlert("OPPAN GANGNAM STYLE!");
        }
    }

    private void haviaPeli() {
        if (!this.peliLoppu) {
            this.peliLoppui();
            soitaAani(Aani.VAROITUS, false);
            this.showAlert("VAROKAA HEIKKOA JÄÄTÄ!");
        }
    }

    private void kaveleJaalla() {
        final float distance = playerNode.getWorldTranslation().distance(houseNode.getWorldTranslation());
        float todnak = 1.5f / distance;
        // System.out.println("distance: " + distance + ", todnak: " + todnak);
        if (FastMath.nextRandomFloat() < 0.005f) {
            this.soitaAani(Aani.HATA, false);
        }
        if (FastMath.nextRandomFloat() < todnak) { //TODO
            ice.hajoa(playerNode.getWorldTranslation());
            this.soitaAani(Aani.HATA, false);
            this.haviaPeli();
        }
    }

    private void peliLoppui() {
        this.peliLoppu = true;
        // getPhysicsSpace().removeCollisionListener(this);
    }
}
