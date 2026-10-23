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
    private final float[] quad=new float[20];
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
    void node(SpriteBatch batch,Planet node,double angle,double extension){
        float r=node.viewPosition.radius,x=node.viewPosition.center.x,y=node.viewPosition.center.y;
        TextureRegion region=nodes[SettingsManager.getInstance().colorblindMode?1:0][node.fraction.ordinal()][node.isDefensive()?1:0];
        float extent=r*4/3;batch.setColor(Color.WHITE);
        if(extension<.001){batch.draw(region,x-extent,y-extent,2*extent,2*extent);return;}
        // Only the transient contact sector deforms. The rest keeps the exact approved atlas and fixed light.
        double dx=Math.cos(angle),dy=Math.sin(angle);
        int steps=r<48?12:r<90?16:20,stride=steps+1;
        // Evaluate shared corners once, then submit the entire mesh together.
        for(int row=0;row<=steps;row++)for(int col=0;col<=steps;col++){
            double u=col/(double)steps,v=row/(double)steps;
            double lx=(u*2-1)*extent,ly=(v*2-1)*extent,along=(lx*dx+ly*dy)/r,across=(-lx*dy+ly*dx)/(r*.48);
            double square=across*across,shift=extension*smooth((along-.55)/.45)*Math.exp(-square*square);
            int k=(row*stride+col)*4;
            nodeGrid[k]=(float)(x+lx+dx*shift);nodeGrid[k+1]=(float)(y+ly+dy*shift);
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
        boolean attached=outgoing>.004||incoming>.004;
        double from=attached?(outgoing>incoming?-Math.PI*.68:Math.PI*.32):0,length=attached?Math.PI*1.36:Math.PI*2,dx=Math.cos(angle),dy=Math.sin(angle);
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
    void neck(SpriteBatch batch,double x,double y,double angle,double length,double halfA,double halfB,double attachment,Color color){
        if(yio.tro.opacha.menu.UiMotion.reduced||attachment<.003||length<=0)return;
        double dx=Math.cos(angle),dy=Math.sin(angle),waist=Math.min(halfA,halfB)*attachment*.23;
        double old=halfA;
        for(int i=1;i<=32;i++){
            double a=(i-1)/32.0,b=i/32.0,t=b<.5?b*2:(b-.5)*2;t=t*t*(3-2*t);
            double width=b<.5?halfA+(waist-halfA)*t:waist+(halfB-waist)*t;
            for(int j=0;j<4;j++){int k=j*5;double d=j<2?a:b,side=j==0?old:j==1?-old:j==2?-width:width;quad[k]=(float)(x+dx*length*d-dy*side);quad[k+1]=(float)(y+dy*length*d+dx*side);quad[k+2]=color.toFloatBits();quad[k+3]=UiStyle.get().pixel.getU();quad[k+4]=UiStyle.get().pixel.getV();}
            batch.draw(UiStyle.get().pixel.getTexture(),quad,0,20);old=width;
        }
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
