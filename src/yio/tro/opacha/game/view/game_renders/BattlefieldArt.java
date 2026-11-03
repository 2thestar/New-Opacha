package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.Gdx;
import yio.tro.opacha.Fonts;
import yio.tro.opacha.SettingsManager;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.UiStyle;

/** Shared managed atlas and small top-down shapes. Material light stays upper-left. */
final class BattlefieldArt {
    private static BattlefieldArt instance;
    private final Texture texture,vaporTexture,roundedTexture,hullTexture;
    private final TextureRegion[][][] nodes;
    private final TextureRegion vapor;
    private final NinePatch rounded,hull;
    private final GlyphLayout glyph=new GlyphLayout();
    private final float[] liquidVertices=new float[20*20*3];
    private final float[] nodeVertices=new float[20*20*20];
    private final float[] nodeGrid=new float[21*21*4];
    static final Color NUMBER=Color.valueOf("171C34"),TREAD=Color.valueOf("849687"),
            BUTTER=Color.valueOf("EAD8A6"),SAGE=Color.valueOf("A5BAB1"),HOT=Color.valueOf("E9B486");
    private final Color scratch=new Color();
    private static final Color TANK_TREAD=Color.valueOf("4B6554"),TANK_RIDGE=Color.valueOf("A0B6A0"),
            RIG_TOP=Color.valueOf("C5CBAA"),RIG_DRUM=Color.valueOf("9BB4AC"),RIG_INTAKE=Color.valueOf("7FACAE"),
            RIG_RIDGE=Color.valueOf("EDF2D9"),RIG_COUNT=Color.valueOf("B0E6E9"),RIG_EMPTY=Color.valueOf("E9E7D9"),
            STEAM=Color.valueOf("D1BBA7"),NIGHT_STEAM=Color.valueOf("EAD7BE"),DAY_HOT=Color.valueOf("D88D56"),NIGHT_HOT=Color.valueOf("FFC18B");
    private static final int CELL=256;
    private BattlefieldArt(){
        FractionType[] fractions=FractionType.values();
        texture=load("nodes.png");vaporTexture=load("vapor.png");roundedTexture=load("rounded.png");hullTexture=load("rig-hull.png");
        vapor=new TextureRegion(vaporTexture);rounded=new NinePatch(roundedTexture,16,16,16,16);hull=new NinePatch(hullTexture,48,48,48,48);
        nodes=new TextureRegion[2][fractions.length][2];
        for(int mode=0;mode<2;mode++)for(FractionType f:fractions)for(int tower=0;tower<2;tower++)
            nodes[mode][f.ordinal()][tower]=new TextureRegion(texture,f.ordinal()*CELL,(mode*2+tower)*CELL,CELL,CELL);
    }
    private Texture load(String name){Texture t=new Texture(Gdx.files.internal("game/battlefield/"+name),true);t.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);return t;}
    static BattlefieldArt get(){if(instance==null)instance=new BattlefieldArt();return instance;}
    static void dispose(){if(instance==null)return;instance.texture.dispose();instance.vaporTexture.dispose();instance.roundedTexture.dispose();instance.hullTexture.dispose();instance=null;}
    static Color face(FractionType f){
        switch(f){case green:return LIME;case cyan:return BLUE;case red:return ROSE;case yellow:return YELLOW;default:return PALE;}
    }
    private static final Color LIME=Color.valueOf("BAF196"),BLUE=Color.valueOf("77E5F2"),
            ROSE=Color.valueOf("FFA8B3"),YELLOW=Color.valueOf("FFDC83"),PALE=Color.valueOf("F3F0EB");
    private static final Color MATTE_LIME=Color.valueOf("C3E7A9"),MATTE_CYAN=Color.valueOf("9AE1EA"),
            MATTE_ROSE=Color.valueOf("F1B6C0"),MATTE_YELLOW=Color.valueOf("EED99F"),MATTE_VIOLET=Color.valueOf("CBA4ED"),
            EDGE_LIME=Color.valueOf("91BF74"),EDGE_CYAN=Color.valueOf("65BBC9"),EDGE_ROSE=Color.valueOf("CB8796"),
            EDGE_YELLOW=Color.valueOf("C6AA68"),EDGE_VIOLET=Color.valueOf("9773B5");
    static final Color SHIELD=Color.valueOf("147B9B"),SHIELD_LIGHT=Color.valueOf("85EDFC");
    static final double SHOT_RELEASE=.09,SHOT_FLIGHT=.24;
    private static final Color FUSE=Color.valueOf("655244"),FUSE_LIGHT=Color.valueOf("D7C29F"),
            SPARK=Color.valueOf("FFD467"),SPARK_HOT=Color.valueOf("F29B54"),BALL=Color.valueOf("24242A"),
            BALL_LIGHT=Color.valueOf("73757C");
    private final double[] tentaclePoints=new double[13*3];
    // Same faction colors as the existing colorblind unit atlas (yellow becomes violet).
    private static final Color ACCESS_CYAN=Color.valueOf("7AE7E7"),ACCESS_GREEN=Color.valueOf("B1E67B"),
            ACCESS_RED=Color.valueOf("E8797A"),ACCESS_YELLOW=Color.valueOf("B57AE7");
    static Color troopFace(FractionType f){
        switch(f){case cyan:return MATTE_CYAN;case green:return MATTE_LIME;case red:return MATTE_ROSE;
            case yellow:return SettingsManager.getInstance().colorblindMode?MATTE_VIOLET:MATTE_YELLOW;default:return PALE;}
    }
    static Color troopEdge(FractionType f){
        switch(f){case cyan:return EDGE_CYAN;case green:return EDGE_LIME;case red:return EDGE_ROSE;
            case yellow:return SettingsManager.getInstance().colorblindMode?EDGE_VIOLET:EDGE_YELLOW;default:return TREAD;}
    }
    private static Color nodeFace(FractionType faction){
        return faction==FractionType.yellow&&SettingsManager.getInstance().colorblindMode?MATTE_VIOLET:face(faction);
    }
    void orderArrow(SpriteBatch batch,double x,double y,double angle,double size,FractionType faction,float alpha){
        scratch.set(troopEdge(faction));scratch.mul(.65f);scratch.a=1;
        arrowShape(batch,x,y,angle,size*1.15,scratch,alpha);
        arrowShape(batch,x,y,angle,size,nodeFace(faction),alpha);
    }
    private void arrowShape(SpriteBatch batch,double x,double y,double angle,double size,Color color,float alpha){
        float half=(float)(size*24/36);
        batch.setColor(color.r,color.g,color.b,alpha);
        batch.draw(UiStyle.get().orderArrow,(float)x-half,(float)y-half,half,half,half*2,half*2,1,1,(float)Math.toDegrees(angle));
        batch.setColor(Color.WHITE);
    }
    void node(SpriteBatch batch,Planet node,double angle,double extension,double time){
        float r=node.viewPosition.radius,x=node.viewPosition.center.x,y=node.viewPosition.center.y;
        TextureRegion region=nodes[SettingsManager.getInstance().colorblindMode?1:0][node.fraction.ordinal()][node.isDefensive()?1:0];
        float extent=r*4/3;batch.setColor(Color.WHITE);
        boolean factory=node.isEconomic(),reduced=yio.tro.opacha.menu.UiMotion.reduced;
        if(extension<.001&&!factory){batch.draw(region,x-extent,y-extent,2*extent,2*extent);return;}
        // Deform the existing material, keeping its light and the count independent.
        double phase=reduced?0:time*.90+node.position.center.x*.013+node.position.center.y*.017;
        double breath=factory&&!reduced?.028*Math.sin(phase*2.3):0;
        double dx=Math.cos(angle),dy=Math.sin(angle);
        int steps=r<48?12:r<90?16:20,stride=steps+1;
        // Evaluate shared corners once, then submit the entire mesh together.
        for(int row=0;row<=steps;row++)for(int col=0;col<=steps;col++){
            double u=col/(double)steps,v=row/(double)steps;
            double lx=(u*2-1)*extent,ly=(v*2-1)*extent,along=(lx*dx+ly*dy)/r,across=(-lx*dy+ly*dx)/(r*.48);
            double square=across*across,shift=extension>.001?extension*smooth((along-.55)/.45)*Math.exp(-square*square):0;
            double scale=1;
            if(factory){
                double radius=Math.hypot(lx,ly)/r,a=Math.atan2(ly,lx);
                double wave=.078*Math.sin(5*a-phase)+.027*Math.sin(3*a+phase*.63);
                scale+=smooth((radius-.30)/.70)*(wave+breath);
            }
            int k=(row*stride+col)*4;
            nodeGrid[k]=(float)(x+lx*scale+dx*shift);nodeGrid[k+1]=(float)(y+ly*scale+dy*shift);
            nodeGrid[k+2]=(float)(region.getU()+u*(region.getU2()-region.getU()));
            nodeGrid[k+3]=(float)(region.getV2()+v*(region.getV()-region.getV2()));
        }
        int offset=0;
        for(int row=0;row<steps;row++)for(int col=0;col<steps;col++){
            for(int corner=0;corner<4;corner++){
                int k=((row+(corner>=2?1:0))*stride+col+(corner==1||corner==2?1:0))*4;
                nodeVertices[offset++]=nodeGrid[k];nodeVertices[offset++]=nodeGrid[k+1];
                nodeVertices[offset++]=Color.WHITE_FLOAT_BITS;
                nodeVertices[offset++]=nodeGrid[k+2];nodeVertices[offset++]=nodeGrid[k+3];
            }
        }
        batch.draw(texture,nodeVertices,0,offset);
    }
    /** A bomb retains the quiet fleet body. Its fuse supplies the distinction. */
    void bomb(SpriteBatch batch,double x,double y,double rx,double angle,FractionType faction,double time,String crew){
        double ry=rx*.73,dx=Math.cos(angle),dy=Math.sin(angle);
        ellipse(batch,x,y,rx+rx*.065,ry+rx*.065,angle,UiStyle.BOARD,1);
        ellipse(batch,x,y,rx,ry,angle,troopFace(faction),1);
        ovalOutline(batch,x,y,rx,ry,angle,rx*.075,troopEdge(faction),0,0);
        double oldX=x+dx*rx*.48-dy*ry*.72,oldY=y+dy*rx*.48+dx*ry*.72;
        for(int i=1;i<=8;i++){
            double t=i/8.0,localX=rx*(.48+.90*t-.23*t*t),localY=ry*(.72+1.58*t-.68*t*t);
            double nextX=x+dx*localX-dy*localY,nextY=y+dy*localX+dx*localY;
            UiStyle.get().line(batch,oldX,oldY,nextX,nextY,rx*.105,FUSE,1);
            UiStyle.get().line(batch,oldX-rx*.014,oldY+rx*.025,nextX-rx*.014,nextY+rx*.025,rx*.035,FUSE_LIGHT,.8f);
            oldX=nextX;oldY=nextY;
        }
        boolean reduced=yio.tro.opacha.menu.UiMotion.reduced;
        double phase=reduced?0:time*5.5+x*.017+y*.013;
        ellipse(batch,oldX,oldY,rx*.13,rx*.13,0,SPARK_HOT,.7f);
        ellipse(batch,oldX,oldY,rx*.075,rx*.075,0,SPARK,1);
        if(!reduced)for(int i=0;i<3;i++){
            double age=(phase+i/3.0)%1,a=angle+.50+i*1.9,d=rx*(.10+.38*age);
            UiStyle.get().line(batch,oldX+Math.cos(a)*d,oldY+Math.sin(a)*d,
                    oldX+Math.cos(a)*(d+rx*.09),oldY+Math.sin(a)*(d+rx*.09),rx*.045,SPARK,(float)(1-age));
        }
        if(crew!=null)count(batch,crew,x,y,ry*1.2,rx*1.5);
    }
    /** A soft overhead tube grows directly from the node rim, leaving its count clear. */
    void tentacle(SpriteBatch batch,Planet node,double angle,double age,boolean selected){
        boolean reduced=yio.tro.opacha.menu.UiMotion.reduced;
        double pulse=!reduced&&age>=0&&age<.29?(age<SHOT_RELEASE?smooth(age/SHOT_RELEASE):1-smooth((age-SHOT_RELEASE)/.20)):0;
        double recoil=!reduced&&age>=SHOT_RELEASE&&age<.30?.11*(1-smooth((age-SHOT_RELEASE)/.21)):0;
        double r=node.viewPosition.radius,x=node.viewPosition.center.x,y=node.viewPosition.center.y,dx=Math.cos(angle),dy=Math.sin(angle);
        double tip=1.75-recoil,armedScale=selected?1.5:1,root=.84+.26*armedScale;
        for(int i=0;i<=12;i++){
            double u=i/12.0,along=r*(root+(tip-root)*u),bend=r*.18*Math.pow(Math.sin(Math.PI*u),2);
            tentaclePoints[i*3]=x+dx*along-dy*bend;
            tentaclePoints[i*3+1]=y+dy*along+dx*bend;
            tentaclePoints[i*3+2]=r*(.52-.14*u)*armedScale*(1+.16*pulse*Math.sin(Math.PI*u));
        }
        Color edge=troopEdge(node.fraction),face=troopFace(node.fraction);
        for(int layer=0;layer<3;layer++)for(int i=1;i<=12;i++){
            double offsetX=layer==2?-r*.028:0,offsetY=layer==2?r*.040:0;
            double width=tentaclePoints[i*3+2]-(layer==1?r*.065:0);
            if(layer==2)width=r*.060;
            UiStyle.get().line(batch,tentaclePoints[(i-1)*3]+offsetX,tentaclePoints[(i-1)*3+1]+offsetY,
                    tentaclePoints[i*3]+offsetX,tentaclePoints[i*3+1]+offsetY,width,
                    layer==0?edge:layer==1?face:Color.WHITE,layer==2?.36f:1);
        }
        double mx=x+dx*r*tip,my=y+dy*r*tip,opening=.60+.40*pulse,thickness=selected?1.5:1;
        ellipse(batch,mx,my,r*.105,r*.235*thickness,angle,edge,1);
        ellipse(batch,mx-dx*r*.012,my-dy*r*.012,r*.075,r*.202*thickness,angle,face,1);
        ellipse(batch,mx+dx*r*.012,my+dy*r*.012,r*.045*opening,r*.145*opening*thickness,angle,BALL,1);
    }
    void cannonBall(SpriteBatch batch,double x,double y,double radius){
        if(UiStyle.isNight())ellipse(batch,x,y,radius*1.30,radius*1.30,0,UiStyle.OUTLINE,.95f);
        ellipse(batch,x,y,radius,radius,0,BALL,1);
        ellipse(batch,x-radius*.27,y+radius*.30,radius*.21,radius*.16,-.4,BALL_LIGHT,.7f);
    }
    void ellipse(SpriteBatch batch,double x,double y,double rx,double ry,double angle,Color color,float alpha){
        batch.setColor(color.r,color.g,color.b,alpha);TextureRegion t=UiStyle.get().circle;
        batch.draw(t,(float)(x-rx),(float)(y-ry),(float)rx,(float)ry,(float)(2*rx),(float)(2*ry),1,1,(float)Math.toDegrees(angle));batch.setColor(Color.WHITE);
    }
    private void box(SpriteBatch batch,double x,double y,double width,double height,double radius,double angle,Color color){
        patch(batch,rounded,16,x,y,width,height,radius,angle,color);
    }
    private void patch(SpriteBatch batch,NinePatch patch,double corner,double x,double y,double width,double height,double radius,double angle,Color color){
        double scale=radius/corner,ww=width/scale,hh=height/scale;
        batch.setColor(color);patch.draw(batch,(float)(x-ww/2),(float)(y-hh/2),(float)ww/2,(float)hh/2,(float)ww,(float)hh,(float)scale,(float)scale,(float)Math.toDegrees(angle));batch.setColor(Color.WHITE);
    }
    void smallBox(SpriteBatch batch,double x,double y,double width,double height,double radius,double angle,Color color){box(batch,x,y,width,height,radius,angle,color);}
    void ovalOutline(SpriteBatch batch,double x,double y,double rx,double ry,double angle,double width,Color color,double outgoing,double incoming){
        double attachment=Math.max(outgoing,incoming),gap=attachment>.03?1.30*attachment:0;
        double from=gap>0?(outgoing>incoming?-Math.PI+gap:gap):0,length=Math.PI*2-2*gap,dx=Math.cos(angle),dy=Math.sin(angle);
        for(int i=0;i<32;i++){
            double a=from+length*i/32,b=from+length*(i+1)/32;
            UiStyle.get().line(batch,x+dx*rx*Math.cos(a)-dy*ry*Math.sin(a),y+dy*rx*Math.cos(a)+dx*ry*Math.sin(a),
                    x+dx*rx*Math.cos(b)-dy*ry*Math.sin(b),y+dy*rx*Math.cos(b)+dx*ry*Math.sin(b),width,color,1);
        }
    }
    void count(SpriteBatch batch,String value,double x,double y,double height,double maxWidth){
        label(batch,value,x,y,height,maxWidth,NUMBER);
    }
    void label(SpriteBatch batch,String value,double x,double y,double height,double maxWidth,Color color){
        BitmapFont font=Fonts.unitFont;float sx=font.getData().scaleX,sy=font.getData().scaleY;
        float scale=(float)(sx*height/font.getCapHeight());font.getData().setScale(scale);
        font.setColor(color);glyph.setText(font,value);if(glyph.width>maxWidth){scale*=maxWidth/glyph.width;font.getData().setScale(scale);glyph.setText(font,value);}
        font.draw(batch,glyph,(float)(x-glyph.width/2),(float)(y+glyph.height/2));
        font.getData().setScale(sx,sy);font.setColor(UiStyle.INK);batch.setColor(Color.WHITE);
    }
    /** One continuous, rounded opening: a broad bulge tightens to a waist, then seals. */
    void liquidBridge(SpriteBatch batch,double x,double y,double radius,double angle,double distance,double rx,double ry,
                      double attachment,FractionType nodeFaction,FractionType fleetFaction,double zoom){
        if(yio.tro.opacha.menu.UiMotion.reduced||attachment<=.03)return;
        double gap=1.30*attachment,start=radius*.60,end=distance-rx*Math.cos(gap),length=end-start;
        if(length<=0)return;
        double root=radius*.54*Math.sqrt(attachment),tip=ry*Math.sin(gap);
        double waist=Math.min(root,tip)*(.18+.82*Math.pow(attachment,1.5));
        double slope=ry/rx*Math.cos(gap)/Math.max(.02,Math.sin(gap));
        double control=Math.max(waist,tip-Math.min(tip*.80,slope*length/6));
        double dx=Math.cos(angle),dy=Math.sin(angle),old=root;
        Color from=nodeFace(nodeFaction),to=troopFace(fleetFaction),edge=troopEdge(fleetFaction);
        TextureRegion pixel=UiStyle.get().pixel;double widthStroke=Gdx.graphics.getWidth()*.0018;int offset=0;
        int steps=radius/zoom<20?8:20;
        float oldBits=from.toFloatBits(),edgeBits=edge.toFloatBits();
        for(int i=1;i<=steps;i++){
            double a=(i-1)/(double)steps,b=i/(double)steps,t=b<.5?b*2:(b-.5)*2,u=1-t;
            double width=b<.5?root+(waist-root)*smooth(t):u*u*u*waist+3*u*u*t*waist+3*u*t*t*control+t*t*t*tip;
            float bits=scratch.set(from).lerp(to,(float)smooth(b)).toFloatBits();
            for(int j=0;j<4;j++){
                double at=j<2?a:b,side=j==0?old:j==1?-old:j==2?-width:width;
                liquidVertices[offset++]=(float)(x+dx*(start+length*at)-dy*side);
                liquidVertices[offset++]=(float)(y+dy*(start+length*at)+dx*side);
                liquidVertices[offset++]=j<2?oldBits:bits;liquidVertices[offset++]=pixel.getU();liquidVertices[offset++]=pixel.getV();
            }
            double along=length*(b-a),difference=width-old,normal=Math.hypot(along,difference);
            for(int side=-1;side<=1;side+=2){
                double nx=-difference*side/normal*widthStroke/2,ny=along/normal*widthStroke/2;
                for(int j=0;j<4;j++){
                    boolean first=j<2;double direction=(j==0||j==3)?1:-1;
                    double px=start+length*(first?a:b)+nx*direction,py=(first?old:width)*side+ny*direction;
                    liquidVertices[offset++]=(float)(x+dx*px-dy*py);liquidVertices[offset++]=(float)(y+dy*px+dx*py);
                    liquidVertices[offset++]=edgeBits;liquidVertices[offset++]=pixel.getU();liquidVertices[offset++]=pixel.getV();
                }
            }
            old=width;oldBits=bits;
        }
        batch.draw(pixel.getTexture(),liquidVertices,0,offset);
    }
    void mergeRemainder(SpriteBatch batch,Planet node,FractionType faction,int value,double angle,double age,double zoom){
        double radius=node.viewPosition.radius,base=Math.min(Gdx.graphics.getWidth()*.038,Gdx.graphics.getWidth()*(.020+Math.sqrt(value)*.0012));
        double u=smooth(age/.28),scale=Math.sqrt(1-u),rx=base*1.15*scale,ry=base*.73/1.15*scale;
        if(rx<.01||ry<.01)return;
        double distance=radius+base*.82-(base*1.15+radius*.30)*u,dx=Math.cos(angle),dy=Math.sin(angle);
        double x=node.viewPosition.center.x+dx*distance,y=node.viewPosition.center.y+dy*distance;
        liquidBridge(batch,node.viewPosition.center.x,node.viewPosition.center.y,radius,angle,distance,rx,ry,1,node.fraction,faction,zoom);
        ellipse(batch,x,y,rx,ry,angle,troopFace(faction),1);
        ovalOutline(batch,x,y,rx,ry,angle,Gdx.graphics.getWidth()*.0019,troopEdge(faction),1,0);
    }
    void tank(SpriteBatch batch,double x,double y,double size,double angle,FractionType faction){
        double dx=Math.cos(angle),dy=Math.sin(angle),s=size/15;
        box(batch,x,y,40*s,30*s,9*s,angle,UiStyle.BOARD);
        for(int side=-1;side<=1;side+=2){
            box(batch,x-dy*9*s*side,y+dx*9*s*side,30*s,6*s,3*s,angle,TANK_TREAD);
            for(int i=0;i<6;i++){
                double at=(-12+i*4.6)*s;
                UiStyle.get().line(batch,x+dx*at-dy*7*s*side,y+dy*at+dx*7*s*side,x+dx*at-dy*11*s*side,y+dy*at+dx*11*s*side,s,TANK_RIDGE,1);
            }
        }
        UiStyle.get().line(batch,x+dx*9*s,y+dy*9*s,x+dx*20*s,y+dy*20*s,4.2*s,troopEdge(faction),1);
        box(batch,x+dx*s,y+dy*s,29.5*s,17.5*s,6.75*s,angle,troopEdge(faction));
        box(batch,x+dx*s,y+dy*s,28*s,16*s,6*s,angle,troopFace(faction));
    }
    void rig(SpriteBatch batch,double x,double y,double size,double angle,int count,double phase,double pocket){
        double dx=Math.cos(angle),dy=Math.sin(angle),s=size/22;
        double rear=Math.min(-22*s,pocket-17*s),front=Math.max(22*s,pocket+17*s),mid=(rear+front)/2,length=front-rear;
        for(int side=-1;side<=1;side+=2){
            box(batch,x+dx*mid-dy*12*s*side,y+dy*mid+dx*12*s*side,length-8*s,8*s,4*s,angle,TREAD);
            for(int i=0;i<5;i++){
                double at=rear+10*s+i*(length-20*s)/4;
                UiStyle.get().line(batch,x+dx*at-dy*10.4*s*side,y+dy*at+dx*10.4*s*side,x+dx*at-dy*13.6*s*side,y+dy*at+dx*13.6*s*side,1.2*s,TANK_RIDGE,1);
            }
        }
        patch(batch,hull,48,x+dx*mid,y+dy*mid,length,26*s,10*s,angle,Color.WHITE);
        box(batch,x+dx*21*s,y+dy*21*s,12*s,18*s,5*s,angle,RIG_TOP);
        box(batch,x+dx*29*s,y+dy*29*s,12*s,15*s,6*s,angle,RIG_DRUM);
        for(int i=0;i<4;i++){
            double a=phase*2.8+i*Math.PI/2,at=(29+Math.sin(a)*3.8)*s;
            UiStyle.get().line(batch,x+dx*at+dy*6.5*s,y+dy*at-dx*6.5*s,x+dx*(at-2*s)-dy*6.5*s,y+dy*(at-2*s)+dx*6.5*s,1.7*s,RIG_RIDGE,(float)(.45+.35*Math.cos(a)));
        }
        ellipse(batch,x+dx*35*s,y+dy*35*s,2.2*s,2.2*s,0,TREAD,1);
        box(batch,x+dx*(rear+.5*s),y+dy*(rear+.5*s),7*s,17*s,3.5*s,angle,RIG_INTAKE);
        box(batch,x+dx*(rear+.75*s),y+dy*(rear+.75*s),4.5*s,13*s,2.2*s,angle,MATTE_CYAN);
        ellipse(batch,x+dx*pocket,y+dy*pocket,14.2*s,9.2*s,0,count==0?RIG_EMPTY:RIG_COUNT,1);
        count(batch,Integer.toString(count),x+dx*pocket,y+dy*pocket,size*.79,size*1.20);
    }
    void puff(SpriteBatch batch,Planet node,double angle,double age){
        if(age<0||age>.56)return;
        double r=node.viewPosition.radius,u=age/.56,x=node.viewPosition.center.x+Math.cos(angle)*r,y=node.viewPosition.center.y+Math.sin(angle)*r;
        Color hot=UiStyle.isNight()?NIGHT_HOT:DAY_HOT,steam=UiStyle.isNight()?NIGHT_STEAM:STEAM;
        if(yio.tro.opacha.menu.UiMotion.reduced){if(age<.18)GameRendersList.getInstance().renderPlanets.arc(node.viewPosition.center.x,node.viewPosition.center.y,r*1.03,angle-.22,angle+.22,r*.08,hot,.8f);return;}
        GameRendersList.getInstance().renderPlanets.arc(node.viewPosition.center.x,node.viewPosition.center.y,r*1.03,angle-.23,angle+.23,r*(.024+.096*(1-u)),hot,(float)(.75*Math.pow(1-u,1.35)));
        for(int i=0;i<5;i++){
            double a=angle+(i-2)*.50,d=r*(.12+(.68+(i%2)*.28)*(1-Math.pow(1-u,3))),size=r*(.148+.232*Math.sin(Math.PI*u))*(i==2?1.12:1);
            batch.setColor(i%2==0?hot:steam);batch.setColor(batch.getColor().r,batch.getColor().g,batch.getColor().b,(float)(.72*Math.pow(1-u,1.35)));
            batch.draw(vapor,(float)(x+Math.cos(a)*d-size),(float)(y+Math.sin(a)*d-size),(float)(size*2),(float)(size*2));
        }
        batch.setColor(Color.WHITE);
        for(int side=-1;side<=1;side+=2){double d=r*(.12+.84*u),across=r*(.08+.44*u)*side;ellipse(batch,x+Math.cos(angle)*d-Math.sin(angle)*across,y+Math.sin(angle)*d+Math.cos(angle)*across,r*(.012+.042*(1-u)),r*(.012+.042*(1-u)),0,hot,(float)(.55*Math.pow(1-u,1.35)));}
    }
    static double smooth(double n){n=Math.max(0,Math.min(1,n));return n*n*(3-2*n);}
}
