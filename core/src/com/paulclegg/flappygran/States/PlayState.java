package com.paulclegg.flappygran.States;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.utils.Array;
import com.paulclegg.flappygran.FlappyGran;
import com.paulclegg.flappygran.Preferences;
import com.paulclegg.flappygran.Sprites.Cake;
import com.paulclegg.flappygran.Sprites.GameScore;
import com.paulclegg.flappygran.Sprites.Gran;
import com.paulclegg.flappygran.Sprites.Tube;

/**
 * Created by cle99 on 26/03/2017.
 */

public class PlayState extends States {

    private static final int TUBE_SPACE = Math.round(FlappyGran.WIDTH * 0.66f);
    private static final int TUBE_POINT = 1;
    private static final int CAKE_POINT = 0;
    private static final int NUMBER_OF_TUBES = 4;


    private Gran gran;
    private Texture bg, ground;
    private Array<Tube> tubes;
    private Array<Cake> cakes;
    private ShapeRenderer sr;
    private BitmapFont font;
    private GlyphLayout layout;
    private SpriteBatch batch;
    public GameScore gameScore;
    private int scoringTube;
    private boolean gameover;



    public PlayState(GameStateManager gsm) {
        super(gsm);
        gameover = false;
        scoringTube = 0;
        gameScore = new GameScore();
        layout = new GlyphLayout();
        font = new BitmapFont(Gdx.files.internal("flappygran.fnt"));
        font.getData().setScale(1.6f);
        gran = new Gran(50, FlappyGran.HEIGHT / 2);
        camera.setToOrtho(false, FlappyGran.WIDTH, FlappyGran.HEIGHT);
        gui.setToOrtho(false, FlappyGran.WIDTH, FlappyGran.HEIGHT);
        bg = new Texture("bg.png");
        ground = new Texture("ground.png");
        tubes = new Array<Tube>();
        cakes = new Array<Cake>();


        for (int i = 0; i < NUMBER_OF_TUBES; i++) {
            tubes.add(new Tube(i * (TUBE_SPACE + Tube.TUBE_WIDTH) + camera.viewportWidth));
            //tubes.get(i).setGap(true);  // open gaps for initial series of tubes
        }
        for (int i = 0; i < NUMBER_OF_TUBES; i++) {
            cakes.add(new Cake(i * (TUBE_SPACE + Tube.TUBE_WIDTH) + camera.viewportWidth - (TUBE_SPACE + Tube.TUBE_WIDTH) / 2));
        }
        sr = new ShapeRenderer();
    }

    @Override
    public void handleInput() {
        if (Gdx.input.justTouched()) {
            gran.jump();
            //gameScore.addToScore(1);
        }
    }

    @Override
    public void update(float dt) {
        handleInput();
        gran.update(dt);
        camera.position.x = gran.getPosition().x + camera.viewportWidth * 0.25f;

        for (int i = 0; i < tubes.size; i++) {
            Tube tube = tubes.get(i);
            Cake cake = cakes.get(i);
            //tube.setGap(cake.isEaten());

            if (camera.position.x - camera.viewportWidth / 2 > tube.getPosTopTube().x + Tube.TUBE_WIDTH) {
                tube.reposition(tube.getPosTopTube().x + (Tube.TUBE_WIDTH + TUBE_SPACE) * NUMBER_OF_TUBES);
                cake.reposition(cake.getPosition().x + (Tube.TUBE_WIDTH + TUBE_SPACE) * NUMBER_OF_TUBES);
            }

            if (tubes.get(scoringTube).getPosBottomTube().x + Tube.TUBE_WIDTH < gran.getPosition().x ) {
                gameScore.addToScore(TUBE_POINT);
                tube.incrementTubesPassed();
                if (scoringTube < tubes.size - 1) {
                    scoringTube++; ;
                } else {
                    scoringTube = 0;
                }
            }

            if (tube.collides(gran.getBounds("bottom")) ||
                    tube.collides(gran.getBounds("top")) ||
                    gran.getPosition().y < FlappyGran.HEIGHT * 0.2f) {
                gameover = true;
                gsm.set(new GameOver(gsm, gameScore.getScore()));
                tube.resetTubes();
                if (gameScore.getScore() > Preferences.getHighScore()) {
                    Preferences.setHighScore(gameScore.getScore());
                }
                gameScore.reset();
            }

            if (!cakes.get(scoringTube).isEaten() && tube.getTubesPassed() > 3) {
                if (Intersector.overlaps(gran.getBounds("bottom"), cakes.get(scoringTube).getBounds())) {
                    gameScore.addToScore(CAKE_POINT);
                    cakes.get(scoringTube).nowEaten();
                    tubes.get(scoringTube).setGap(cakes.get(scoringTube).isEaten());
                }
            }
        }
        camera.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setProjectionMatrix(camera.combined);
        sb.begin();
        sb.draw(bg, camera.position.x - camera.viewportWidth / 2, 0, camera.viewportWidth, camera.viewportHeight);

        for (Tube tube : tubes) {
            sb.draw(tube.getBotttomTube(), tube.getPosBottomTube().x, tube.getPosBottomTube().y,
                    tube.TUBE_WIDTH, tube.getBotttomTube().getHeight());
            sb.draw(tube.getTopTube(), tube.getPosTopTube().x, tube.getPosTopTube().y,
                    tube.TUBE_WIDTH, tube.getTopTube().getHeight());
        }

        for (Cake cake : cakes) {
            if (!cake.isEaten()) {
                if (tubes.get(0).getTubesPassed() > 3) {
                    sb.draw(cake.getCake(), cake.getPosition().x, cake.getPosition().y);
                }
            }
        }

        sb.draw(ground, camera.position.x - camera.viewportWidth /2, 0, camera.viewportWidth, camera.viewportHeight * 0.2f);
        sb.draw(gran.getGran(), gran.getPosition().x, gran.getPosition().y,
                Gran.PLAYER_SIZE, Gran.PLAYER_SIZE);
        sb.end();

        // gui camera below (fixed)

        sb.setProjectionMatrix(gui.combined);
        sb.begin();
        layout.setText(font, Preferences.highScoreToString());
        font.draw(sb, Preferences.highScoreToString(),
                (FlappyGran.WIDTH  - layout.width) / 2,
                camera.viewportHeight * 0.075f);
        layout.setText(font, gameScore.scoreToString());
        font.draw(sb, layout,
                (FlappyGran.WIDTH  - layout.width) / 2,
                camera.viewportHeight * 0.175f);
        sb.end();

//        sr.setProjectionMatrix(camera.combined);
//        sr.begin(ShapeRenderer.ShapeType.Line);
//        sr.setColor(Color.BLACK);
//        sr.circle(gran.getBounds("botttom").x, gran.getBounds("bottom").y, gran.getBounds("bottom").radius);
//
//        sr.circle(gran.getBounds("top").x, gran.getBounds("top").y, gran.getBounds("top").radius);
//        for (int i = 0; i < tubes.size; i++) {
//            Tube tube = tubes.get(i);
//            Cake cake = cakes.get(i);
//            sr.rect(tube.getTopBounds().x, tube.getBottomBounds().y,
//                    tube.TUBE_WIDTH, tube.getBotttomTube().getHeight());
//            sr.circle(cake.getBounds().x, cake.getBounds().y,
//                    cake.getSize());
//        }
//        sr.end();

    }

    @Override
    public void dispose() {
        bg.dispose();
        gran.dispose();
        for (Tube tube : tubes) {
            tube.dispose();
        }
        for (Cake cake :cakes) {
            cake.dispose();
        }
    }
}
