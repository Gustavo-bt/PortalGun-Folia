package pl.by.fentisdev.portalgun.portalgun;

import com.google.gson.JsonObject;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import pl.by.fentisdev.portalgun.PortalGunMain;
import pl.by.fentisdev.portalgun.utils.PortalUtils;
import pl.by.fentisdev.portalgun.utils.SchedulerUtil;

import java.util.ArrayList;
import java.util.List;

public class Portal {

    @Getter
    private Location loc1,loc2;
    private int chunk1x,chunk1z,chunk2x,chunk2z;
    @Getter
    private PortalColors color;
    @Getter
    private BlockFace face;
    @Getter
    private ItemFrame up;
    @Getter
    private ItemFrame down;
    @Getter
    private BlockFace direction;

    public Portal(PortalColors color){
        this.color = color;
    }

    public void setColor(PortalColors color) {
        this.color = color;
        if (up!=null){
            up.setItem(PortalUtils.getInstance().getPortalMapItem(PortalSide.UP,color));
        }
        if (down!=null){
            down.setItem(PortalUtils.getInstance().getPortalMapItem(PortalSide.DOWN,color));
        }
    }

    public BlockFace getPortalFace() {
        return face;
    }

    public boolean hasPortal(){
        return loc1!=null;
    }

    public boolean inChunk(Chunk chunk){
        return (chunk.getX()==chunk1x&&chunk.getZ()==chunk1z) || (chunk.getX()==chunk2x&&chunk.getZ()==chunk2z);
    }

    public Location getLocTeleport(Entity entity){
        BoundingBox box = entity.getBoundingBox();
        switch (face){
            case DOWN:
                return loc1.getBlock().getLocation().add(0.5,(-box.getHeight())+0.7,0.5);
            case UP:
                return loc1.getBlock().getLocation().add(0.5,0.5,0.5);
            case EAST:
                return loc1.getBlock().getLocation().add(box.getWidthX(),0,0.5);
            case WEST:
                return loc1.getBlock().getLocation().add(1+(-box.getWidthX()),0,0.5);
            case SOUTH:
                return loc1.getBlock().getLocation().add(0.5,0,box.getWidthZ());
            case NORTH:
                return loc1.getBlock().getLocation().add(0.5,0,1+(-(box.getWidthZ())));
            default:
                return loc1.getBlock().getLocation().add(0.5,0.0,0.5);
        }
    }

    public Location getLocTeleport(){
        switch (face){
            case DOWN:
                return loc1.getBlock().getLocation().add(0.5,-1,0.5);
            case UP:
                return loc1.getBlock().getLocation().add(0.5,0.5,0.5);
            default:
                return loc1.getBlock().getLocation().add(0.5,0.0,0.5);
        }
    }

    public boolean isPortalLocation(Location loc){
        return loc1!=null&&(loc.getBlock().getLocation().distance(loc1.getBlock().getLocation())<1 ||
                loc.getBlock().getLocation().distance(loc2.getBlock().getLocation())<1);
    }

    public boolean isPortalLocation(Location loc, BlockFace face){
        return up!=null&&down!=null&&loc1!=null&&loc2!=null&&
                ((loc.getWorld()==loc1.getWorld()&&(loc.getBlock().getLocation().distance(loc1.getBlock().getLocation())<1 && down.getFacing()==face)) ||
                        (loc.getWorld()==loc2.getWorld()&&(loc.getBlock().getLocation().distance(loc2.getBlock().getLocation())<1 && up.getFacing()==face)));
    }

    public boolean isPortalItemFrame(ItemFrame itemFrame){
        return isPortalItemFrameUp(itemFrame) ||
                isPortalItemFrameDown(itemFrame);
    }

    public boolean isPortalItemFrameUp(ItemFrame itemFrame){
        return up!=null&&itemFrame.getUniqueId()==up.getUniqueId();
    }

    public boolean isPortalItemFrameDown(ItemFrame itemFrame){
        return down!=null&&itemFrame.getUniqueId()==down.getUniqueId();
    }

    public boolean verifyPortal(){
        if (hasPortal()&&up!=null&&down!=null&&(up.isDead()||down.isDead())){
            resetPortal();
            return true;
        }
        return false;
    }

    public List<Entity> getEntityNearby(){
        List<Entity> en = new ArrayList<>();
        double x = face== BlockFace.EAST||face== BlockFace.WEST?0.045:0.1;
        double y = face== BlockFace.UP||face== BlockFace.DOWN?0.045:0.1;
        double z = face== BlockFace.SOUTH||face== BlockFace.NORTH?0.034:0.1;
        if (up!=null){
            for (Entity nearbyEntity : up.getNearbyEntities(x, y, z)) {
                if ((nearbyEntity instanceof LivingEntity || nearbyEntity instanceof Item) &&
                        !PortalGunManager.getInstance().beingHeld(nearbyEntity)){
                    en.add(nearbyEntity);
                }
            }
        }
        if (down!=null){
            for (Entity nearbyEntity : down.getNearbyEntities(x, y, z)) {
                if ((nearbyEntity instanceof LivingEntity || nearbyEntity instanceof Item) &&
                        !en.contains(nearbyEntity) &&
                        !PortalGunManager.getInstance().beingHeld(nearbyEntity)){
                    en.add(nearbyEntity);
                }
            }
        }
        return en;
    }

    public void resetPortal(){
        clearPortal();
        this.loc1=null;
        this.loc2=null;
        this.face=null;
        this.direction=null;
    }

    public void loadPortalData(Location loc1, Location loc2, BlockFace face, BlockFace direction){
        this.loc1 = loc1;
        this.loc2 = loc2;
        this.chunk1x = loc1.getBlockX() >> 4;
        this.chunk1z = loc1.getBlockZ() >> 4;
        this.chunk2x = loc2.getBlockX() >> 4;
        this.chunk2z = loc2.getBlockZ() >> 4;
        this.face = face;
        this.direction = direction;
    }
    public void setPortal(Location loc1, Location loc2, BlockFace face, BlockFace direction){
        this.loc1 = loc1;
        this.loc2 = loc2;
        this.chunk1x = loc1.getBlockX() >> 4;
        this.chunk1z = loc1.getBlockZ() >> 4;
        this.chunk2x = loc2.getBlockX() >> 4;
        this.chunk2z = loc2.getBlockZ() >> 4;
        this.face = face;
        this.direction = direction;
        renderPortal();
    }

    public void renderPortal(){
        if (loc1 == null || loc2 == null || face == null) return;
        final Portal self = this;
        final Rotation rotation = self.face==BlockFace.DOWN? self.direction==BlockFace.SOUTH?Rotation.CLOCKWISE: self.direction==BlockFace.WEST?Rotation.CLOCKWISE_45: self.direction==BlockFace.EAST?Rotation.CLOCKWISE_135:Rotation.NONE: self.face==BlockFace.UP? self.direction==BlockFace.SOUTH?Rotation.CLOCKWISE: self.direction==BlockFace.WEST?Rotation.CLOCKWISE_135: self.direction==BlockFace.EAST?Rotation.CLOCKWISE_45:Rotation.NONE:Rotation.NONE;
        final Class<? extends ItemFrame> c = PortalUtils.getInstance().isGlowItemFrame()? GlowItemFrame.class:ItemFrame.class;
        final BlockFace faceCopy = self.face;
        final PortalColors colorCopy = self.color;
        final Location loc1Copy = self.loc1.clone();
        final Location loc2Copy = self.loc2.clone();
        SchedulerUtil.runAtLocation(PortalGunMain.getInstance(), loc1Copy, () -> {
            try {
                if (self.down != null) { try { self.down.remove(); } catch (Throwable ignored) {} self.down = null; }
                for (Entity e : loc1Copy.getWorld().getNearbyEntities(loc1Copy, 0.5, 0.5, 0.5)) {
                    if (e instanceof ItemFrame) { try { e.remove(); } catch (Throwable ignored) {} }
                }
                self.down = (ItemFrame) loc1Copy.getWorld().spawn(loc1Copy, c);
                self.down.setFacingDirection(faceCopy);
                self.down.setRotation(rotation);
                self.down.setItem(PortalUtils.getInstance().getPortalMapItem(PortalSide.DOWN, colorCopy));
                self.down.setInvulnerable(true);
                if (PortalUtils.getInstance().isInvisibleItemFrame()) self.down.setVisible(false);
                colorCopy.getTeleportSound().playSound(loc1Copy, 1, 1);
            } catch (Throwable ex) {
                PortalGunMain.getInstance().getLogger().warning("[PortalGun] renderPortal down: " + ex.getMessage());
            }
        });
        SchedulerUtil.runAtLocation(PortalGunMain.getInstance(), loc2Copy, () -> {
            try {
                if (self.up != null) { try { self.up.remove(); } catch (Throwable ignored) {} self.up = null; }
                for (Entity e : loc2Copy.getWorld().getNearbyEntities(loc2Copy, 0.5, 0.5, 0.5)) {
                    if (e instanceof ItemFrame) { try { e.remove(); } catch (Throwable ignored) {} }
                }
                self.up = (ItemFrame) loc2Copy.getWorld().spawn(loc2Copy, c);
                self.up.setFacingDirection(faceCopy);
                self.up.setRotation(rotation);
                self.up.setItem(PortalUtils.getInstance().getPortalMapItem(PortalSide.UP, colorCopy));
                self.up.setInvulnerable(true);
                if (PortalUtils.getInstance().isInvisibleItemFrame()) self.up.setVisible(false);
            } catch (Throwable ex) {
                PortalGunMain.getInstance().getLogger().warning("[PortalGun] renderPortal up: " + ex.getMessage());
            }
        });
    }

    public void unRenderPortal(){
        clearPortal();
    }

    private void clearPortal(){
        final ItemFrame fUp = this.up;
        final ItemFrame fDown = this.down;
        this.up = null;
        this.down = null;
        if (fUp == null && fDown == null) return;
        Location ref = null;
        if (fUp != null) ref = fUp.getLocation();
        else if (fDown != null) ref = fDown.getLocation();
        if (ref == null) return;
        final Location fRef = ref;
        SchedulerUtil.runAtLocation(PortalGunMain.getInstance(), fRef, () -> {
            try {
                if (fUp != null) {
                    PortalSound.PORTAL_CLOSE.playSound(fRef, 1, 1);
                    fUp.remove();
                }
                if (fDown != null) fDown.remove();
            } catch (Throwable ignored) {}
        });
    }

    public JsonObject toJson(){
        JsonObject json = new JsonObject();
        if (loc1!=null){
            json.add("loc1",PortalUtils.getInstance().getJsonLocation(loc1));
        }
        if (loc2!=null){
            json.add("loc2",PortalUtils.getInstance().getJsonLocation(loc2));
        }
        if (face!=null){
            json.addProperty("face",face.toString());
        }
        if (direction!=null){
            json.addProperty("direction",direction.toString());
        }
        return json;
    }

    private static boolean isPortalFrame(org.bukkit.entity.Entity e) {
        if (!(e instanceof ItemFrame)) return false;
        ItemStack it = ((ItemFrame) e).getItem();
        if (it == null || it.getType() != Material.FILLED_MAP) return false;
        if (!it.hasItemMeta() || !it.getItemMeta().hasLore()) return false;
        return it.getItemMeta().getLore().contains("Portal");
    }
}
