package io.leonis.subra.game.engine;

import io.leonis.subra.game.formations.PositionFormation;
import io.leonis.zosma.game.engine.Deducer;
import java.util.stream.Collectors;
import lombok.Value;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.reactivestreams.Publisher;
import io.leonis.subra.game.data.*;
import reactor.core.publisher.Flux;

/**
 * The Class BallTrackerFormationDeducer.
 *
 * This class represents a {@link Deducer} which builds a {@link PositionFormation} which
 * places {@link Player} formation positions on a circle around the ball.
 *
 * @param <G> A value object containing a {@link java.util.Set} of {@link Player} and a {@link
 *            java.util.Set}
 * @author Rimon Oz
 */
@Value
public class BallTrackerFormationDeducer<G extends Player.SetSupplier & Ball.Supplier>
    implements Deducer<G, PositionFormation> {
  private final TeamColor teamColor;
  private final double distanceFromBall;

  @Override
  public Publisher<PositionFormation> apply(final Publisher<G> inputPublisher) {
    return Flux.from(inputPublisher)
        .map(game ->
            new PositionFormation(
                game.getPlayers().stream()
                    .filter(player -> player.getTeamColor().equals(this.getTeamColor()))
                    .collect(Collectors.toMap(
                        Player::getIdentity,
                        player ->
                            Nd4j.vstack(
                                game.getBall().getPosition()
                                    .add(Transforms.unitVec(
                                        game.getBall().getPosition()
                                            .get(NDArrayIndex.interval(0, 2), NDArrayIndex.all())
                                            .sub(player.getPosition()
                                                .get(NDArrayIndex.interval(0, 2), NDArrayIndex.all())))
                                        .mul(this.getDistanceFromBall())))))));
  }
}
