/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

/**
 *
 * @author lime
 */
class Ice extends Node {

    private final Spatial spatial;
    private final CollisionShape collisionShape;
    private final RigidBodyControl control;

//    public Ice() {
//        super("Ice", new Box(500, 0, 500));
//        Plane plane = new Plane();
//        plane.setOriginNormal(new Vector3f(0, 0.25f, 0), Vector3f.UNIT_Y);
//        this.addControl(new RigidBodyControl(new PlaneCollisionShape(plane), 0));
//    }
    Ice(String name, AssetManager assetManager) {
        super(name);


        spatial = assetManager.loadModel("Models/ice/ice.mesh.j3o");
        spatial.scale(7.0f);
        Material iceMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        iceMaterial.setTexture("ColorMap", assetManager.loadTexture("Textures/ice.jpg"));
        spatial.setMaterial(iceMaterial);

        collisionShape = CollisionShapeFactory.createMeshShape((Node) spatial);
        control = new RigidBodyControl(collisionShape, 0);
        
        this.addControl(control);
        this.attachChild(spatial);
    }

    public void setLocation(Vector3f v) {
        control.setPhysicsLocation(v);
    }
    
    public Vector3f getLocation(){
        return control.getPhysicsLocation();
    }

    public void hajoa(Vector3f globalLocation) {
        Vector3f iceLocation = new Vector3f(globalLocation.getX()+2.0f, this.getLocation().getY(), globalLocation.getZ()+3.0f);
        this.setLocation(iceLocation);
    }
}
