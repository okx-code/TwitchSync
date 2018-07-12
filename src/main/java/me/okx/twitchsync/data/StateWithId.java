package me.okx.twitchsync.data;

public class StateWithId implements Comparable<StateWithId> {
  private CheckState state;
  private int id;

  public StateWithId(CheckState state, int id) {
    this.state = state;
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public CheckState getState() {
    return state;
  }

  @Override
  public int compareTo(StateWithId stateWithId) {
    return this.state.compareTo(stateWithId.state);
  }

  @Override
  public String toString() {
    return "StateWithId{state = " + state + ", id = " + id + "}";
  }
}
