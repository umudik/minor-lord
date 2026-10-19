package io.github.umudik.minorlord.mining;
import net.minecraft.core.*;
import java.util.Optional;
/** Two-wide ten-block lead-in joins a four-block-pitch outward spiral (two solid blocks between rings); radius stays centered on the sign. */
public final class MiningPattern {
    private MiningPattern(){}
    public static Optional<BlockPos> point(MiningBounds bounds,Direction forward,long index) {
        if(index<0)throw new IllegalArgumentException("Negative progress");
        double x,z;
        if(index<10){x=index+1;z=0;}
        else {
            double distance=(index-10)*0.5;
            double pitch=4.0/(2*Math.PI);
            double radius=Math.sqrt(100+2*pitch*distance);
            if(radius>bounds.radius())return Optional.empty();
            double angle=(radius-10)/pitch;
            x=radius*Math.cos(angle);z=radius*Math.sin(angle);
        }
        var right=forward.getClockWise();
        long px=(long)bounds.signX()+Math.round(x)*forward.getStepX()+Math.round(z)*right.getStepX();
        long pz=(long)bounds.signZ()+Math.round(x)*forward.getStepZ()+Math.round(z)*right.getStepZ();
        if(px<Integer.MIN_VALUE||px>Integer.MAX_VALUE||pz<Integer.MIN_VALUE||pz>Integer.MAX_VALUE)return Optional.empty();
        var result=new BlockPos((int)px,bounds.bottomY(),(int)pz);
        return bounds.containsHorizontal(result.getX(),result.getZ())?Optional.of(result):Optional.empty();
    }
    private static final double PITCH=4.0/(2*Math.PI);
    /** Spiral step where turn k crosses the mine direction again (k=0 is the end of the lead-in). */
    public static long crossing(int k){
        if(k<=0)return 10;double r=10+4.0*k;return 10+Math.round((r*r-100)/PITCH);
    }
    /** Turn of the last crossing at or before this step, or -1 while still on the lead-in. */
    public static int crossingBefore(long index){
        if(index<10)return -1;
        int k=(int)Math.floor((Math.sqrt(100+PITCH*(index-10))-10)/4);
        while(crossing(k+1)<=index)k++;
        while(k>0&&crossing(k)>index)k--;
        return Math.max(0,k);
    }
    /** Distance along the mine direction of turn k's crossing. */
    public static int crossingDistance(int k){return 10+4*Math.max(0,k);}
    /** A cell of the straight spoke: x blocks out along the mine direction, side 0 or 1 to its right. */
    public static BlockPos spoke(MiningBounds bounds,Direction forward,int x,int side){
        var right=forward.getClockWise();
        return new BlockPos(bounds.signX()+x*forward.getStepX()+side*right.getStepX(),bounds.bottomY(),bounds.signZ()+x*forward.getStepZ()+side*right.getStepZ());
    }
    /** The corridor's second column: right of the lead-in, then radially outward on the spiral. */
    public static BlockPos partner(MiningBounds bounds,Direction forward,BlockPos cell,long index){
        if(index<10)return cell.relative(forward.getClockWise());
        long dx=(long)cell.getX()-bounds.signX(),dz=(long)cell.getZ()-bounds.signZ();
        return Math.abs(dx)>=Math.abs(dz)?cell.offset((int)Long.signum(dx),0,0):cell.offset(0,0,(int)Long.signum(dz));
    }
    public static int ticks(float hardness,float speed,boolean correctTool) {
        if(hardness<0 || !Float.isFinite(hardness))return Integer.MAX_VALUE;
        if(hardness==0)return 1;
        return (int)Math.min(Integer.MAX_VALUE,Math.ceil(hardness*(correctTool?30:100)/Math.max(1,speed)));
    }
    /**
     * Every column opened for one step: the path cell and its second column, and at a diagonal step both corner
     * cells with their second columns, so the corridor is never narrower than two blocks. Cells outside the radius
     * are left out.
     */
    public static java.util.List<BlockPos> stepCells(MiningBounds bounds,Direction forward,long index){
        var point=point(bounds,forward,index);if(point.isEmpty())return java.util.List.of();
        var cells=new java.util.LinkedHashSet<BlockPos>();var target=point.get();
        if(index<10){add(cells,bounds,target);add(cells,bounds,partner(bounds,forward,target,index));}
        else brush(cells,bounds,target);
        if(index>0){
            var previous=point(bounds,forward,index-1).orElse(target);
            if(previous.getX()!=target.getX()&&previous.getZ()!=target.getZ())
                for(var corner:java.util.List.of(new BlockPos(target.getX(),target.getY(),previous.getZ()),new BlockPos(previous.getX(),target.getY(),target.getZ())))
                    if(index<10){add(cells,bounds,corner);add(cells,bounds,partner(bounds,forward,corner,index));}else brush(cells,bounds,corner);
        }
        // Crossing the mine direction again: the wall back to the previous ring is dug through, two wide, so a
        // straight spoke runs from the sign to the outermost ring and walking out or home never follows the spiral.
        int turn=crossingBefore(index);
        if(turn>=1&&crossing(turn)==index)
            for(int x=crossingDistance(turn-1);x<=crossingDistance(turn);x++)for(int side=0;side<=1;side++)add(cells,bounds,spoke(bounds,forward,x,side));
        return java.util.List.copyOf(cells);
    }
    /** On the spiral each cell opens a 2x2 square on its outward side, so even diagonal stretches stay two wide. */
    private static void brush(java.util.Set<BlockPos> cells,MiningBounds bounds,BlockPos cell){
        int sx=cell.getX()>=bounds.signX()?1:-1,sz=cell.getZ()>=bounds.signZ()?1:-1;
        add(cells,bounds,cell);add(cells,bounds,cell.offset(sx,0,0));add(cells,bounds,cell.offset(0,0,sz));add(cells,bounds,cell.offset(sx,0,sz));
    }
    private static void add(java.util.Set<BlockPos> cells,MiningBounds bounds,BlockPos cell){
        if(bounds.containsHorizontal(cell.getX(),cell.getZ()))cells.add(cell);
    }
    /** Exact: whether a column belongs to the corridor, found by checking the steps of the spiral rings near it. */
    public static boolean tunnelAt(MiningBounds bounds,Direction forward,int worldX,int worldZ){
        long dx=(long)worldX-bounds.signX(),dz=(long)worldZ-bounds.signZ();var right=forward.getClockWise();
        double x=dx*forward.getStepX()+dz*forward.getStepZ(),z=dx*right.getStepX()+dz*right.getStepZ();
        if(!bounds.containsHorizontal(worldX,worldZ))return false;
        if(x>=1&&x<=11&&z>=-1&&z<=2&&matches(bounds,forward,worldX,worldZ,0,12))return true;
        if(x>=1&&z>=0&&z<=1)return true; // the straight spoke
        double radius=Math.hypot(x,z);if(radius<8)return false;
        double pitch=PITCH;double angle=Math.atan2(z,x);if(angle<0)angle+=2*Math.PI;
        for(int turn=0;;turn++){
            double ring=10+pitch*(angle+2*Math.PI*turn);
            if(ring>radius+3)return false;
            if(ring<radius-3)continue;
            long center=10+(long)Math.floor((ring*ring-100)/pitch);
            if(matches(bounds,forward,worldX,worldZ,Math.max(0,center-14),center+14))return true;
        }
    }
    private static boolean matches(MiningBounds bounds,Direction forward,int x,int z,long from,long to){
        for(long i=from;i<=to;i++)for(var cell:stepCells(bounds,forward,i))if(cell.getX()==x&&cell.getZ()==z)return true;
        return false;
    }
    /** The tunnel mouth around the sign belongs to the player's base and is never sealed. */
    public static boolean mouth(MiningBounds bounds,Direction forward,int worldX,int worldZ){
        long dx=(long)worldX-bounds.signX(),dz=(long)worldZ-bounds.signZ();var right=forward.getClockWise();
        double x=dx*forward.getStepX()+dz*forward.getStepZ(),z=dx*right.getStepX()+dz*right.getStepZ();
        return x<=0&&x*x+z*z<=6.25;
    }
}
