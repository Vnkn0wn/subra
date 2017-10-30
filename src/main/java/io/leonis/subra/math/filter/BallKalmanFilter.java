package io.leonis.subra.math.filter;

import io.leonis.algieba.filter.KalmanFilter;
import io.leonis.algieba.statistic.SimpleDistribution;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.reactivestreams.Publisher;
import io.leonis.subra.game.data.*;
import io.leonis.zosma.game.engine.Deducer;
import io.leonis.algieba.Temporal;
import reactor.core.publisher.Flux;

/**
 * The Class BallKalmanFilter.
 *
 * @author Rimon Oz
 */
public class BallKalmanFilter<I extends Ball.SetSupplier & Referee.Supplier & Temporal>
    implements Deducer<I, Ball> {

  private final KalmanFilter kalmanFilter = new KalmanFilter();

  public static final INDArray MEASUREMENT_TRANSITION_MATRIX = Nd4j.create(
      new double[]{
          1, 0, 0, 0, 0, 0, 0,
          0, 1, 0, 0, 0, 0, 0,
          0, 0, 1, 0, 0, 0, 0,
          0, 0, 0, 1, 0, 0, 0,
          0, 0, 0, 0, 1, 0, 0,
          0, 0, 0, 0, 0, 1, 0,
          0, 0, 0, 0, 0, 0, 1
      },
      new int[]{7, 7});
  public static final INDArray MEASUREMENT_COVARIANCE_MATRIX = Nd4j.create(
      new double[]{
          0.01f, 0, 0, 0, 0, 0, 0,
          0, 0.01f, 0, 0, 0, 0, 0,
          0, 0, 0.01f, 0, 0, 0, 0,
          0, 0, 0, 0.01f, 0, 0, 0,
          0, 0, 0, 0, 0.01f, 0, 0,
          0, 0, 0, 0, 0, 0.01f, 0,
          0, 0, 0, 0, 0, 0, 0.01f
      },
      new int[]{7, 7});
  public static final INDArray CONTROL_TRANSITION_MATRIX = Nd4j.create(
      new double[]{
          1, 0, 0, 0, 0, 0, 0,
          0, 1, 0, 0, 0, 0, 0,
          0, 0, 1, 0, 0, 0, 0,
          0, 0, 0, 1, 0, 0, 0,
          0, 0, 0, 0, 1, 0, 0,
          0, 0, 0, 0, 0, 1, 0,
          0, 0, 0, 0, 0, 0, 1
      },
      new int[]{7, 7});
  public static final INDArray PROCESS_COVARIANCE_MATRIX = Nd4j.create(
      new double[]{
          0.01f, 0, 0, 0, 0, 0, 0,
          0, 0.01f, 0, 0, 0, 0, 0,
          0, 0, 0.01f, 0, 0, 0, 0,
          0, 0, 0, 0.01f, 0, 0, 0,
          0, 0, 0, 0, 0.01f, 0, 0,
          0, 0, 0, 0, 0, 0.01f, 0,
          0, 0, 0, 0, 0, 0, 0.01f
      },
      new int[]{7, 7});

  @Override
  public Publisher<Ball> apply(final Publisher<I> inputPublisher) {
    return Flux.from(inputPublisher)
        .scan(
            new Ball.State(0, 0, 0, 0, 0, 0, 0),
            (previousResult, input) -> input.getBalls().stream()
                .reduce((previousBall, foundPlayer) ->
                    new Ball.State(
                        this.kalmanFilter.apply(
                            getStateTransitionMatrix(
                                (input.getReferee().getTimestamp()
                                    - previousResult.getTimestamp()) / 1000000d),
                            MEASUREMENT_TRANSITION_MATRIX,
                            CONTROL_TRANSITION_MATRIX,
                            Nd4j.zeros(7, 1),
                            PROCESS_COVARIANCE_MATRIX,
                            new SimpleDistribution(
                                previousBall.getState().getMean(),
                                MEASUREMENT_COVARIANCE_MATRIX),
                            foundPlayer.getState())))
                .orElse(new Ball.State(0, 0, 0, 0, 0, 0, 0)));
  }

  public static INDArray getStateTransitionMatrix(final double timeDifference) {
    return Nd4j.create(
        new double[]{
            1, 0, 0, 0, 0, 0, 0,
            0, 1, 0, 0, timeDifference, 0, 0,
            0, 0, 1, 0, 0, timeDifference, 0,
            0, 0, 0, 1, 0, 0, timeDifference,
            0, 0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0, 1
        },
        new int[]{7, 7});
  }
}
