package library;

public class GamePlay extends AbstractBean implements java.io.Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7006462820149743976L;
	private StateOfRound currentState; 
	private Player currentPlayer; 
	private PlayerContainer playerContainer; 
	private Dealer dealer; 
	
	public enum StateOfRound{
		BETTING	(0, 'B'),
		PLAYER_ACTION(1, 'P'),
		DEALER_ACTION(2, 'D'), 
		COLLECTION(3, 'C');
		
		private final int stateValue; 
		private final Character stateSymbol; 
		
		@SuppressWarnings("unused")
		private static StateOfRound[] stateArray = {BETTING, PLAYER_ACTION, DEALER_ACTION, COLLECTION};  
		
		private StateOfRound(int stateValue, Character stateSymbol)
		{
			this.stateValue = stateValue; 
			this.stateSymbol = stateSymbol; 
		}

		public int getStateValue() {
			return stateValue;
		}

		public Character getStateSymbol() {
			return stateSymbol;
		}
		
		public static StateOfRound next(StateOfRound currentState)
		{
			return values()[(currentState.getStateValue() + 1)%4];
		}
		
		
	}
	
	//add methods for creating players when players join the game
	//This will need to give the players the same dealer.
	

	public void addPlayer(Player newPlayer)
	{
		//TODOSame question as setCurrentPlayer - If I have PCS fire happening in newPlayer.setDealer, and PlayerContainer.addPlayer, and Player.setGamePlay, do i need it here? 
		newPlayer.setDealer(this.getDealer());
		this.getPlayerContainer().addPlayer(newPlayer);
		newPlayer.setGamePlay(this);
	}
	
	public StateOfRound getCurrentState() {
		return currentState;
	}

	public void setCurrentState(StateOfRound currentState) {
		StateOfRound oldState = this.getCurrentState();
		this.currentState = currentState;
		this.getPcs().firePropertyChange("currentState", oldState, this.getCurrentState());
	}
	
	public void nextState(){
		//Tricking java is fun
		StateOfRound oldState = this.currentState;
		this.currentState = StateOfRound.next(currentState);
		this.getPcs().firePropertyChange("currentState", oldState, this.getCurrentState());
	}

	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	//TODO make sure this is the only method that switches the currentPlayer
	//After Bean
	public void setCurrentPlayer(Player currentPlayer) {
		Player oldPlayer = this.currentPlayer;
		this.currentPlayer = currentPlayer;
		this.dealer.setCurrentPlayer(currentPlayer);
		this.playerContainer.setCurrentPlayer(currentPlayer);
		//this may cause the UI to do extra work
		this.getPcs().firePropertyChange("currentPlayer", oldPlayer, this.getCurrentPlayer());
		if(currentPlayer != null){
			currentPlayer.setCurrent(true);
		}
		for (int i = 0; i < playerContainer.getPlayerContainer().size(); i++)
		{
			if (playerContainer.getPlayerContainer().get(i) != currentPlayer)
			{
				playerContainer.getPlayerContainer().get(i).setCurrent(false);
			}
		}
	}

	public PlayerContainer getPlayerContainer() {
		return playerContainer;
	} 

	public void setPlayerContainer(PlayerContainer playerContainer) {
		PlayerContainer oldPlayerContainer = this.getPlayerContainer();
		this.playerContainer = playerContainer;
		this.getDealer().setPlayerContainer(playerContainer);
		this.getPcs().firePropertyChange("playerContainer", oldPlayerContainer, this.getPlayerContainer());
	}

	public Dealer getDealer() {
		return dealer;
	}

	public void setDealer(Dealer dealer) {
		Dealer oldDealer = this.getDealer();
		this.dealer = dealer;
		this.getPcs().firePropertyChange("dealer", oldDealer, this.getDealer());
	}
	
	public void nextCurrentPlayer()
	{
		//need to make sure the servers have these libraries so everyone is in sync
		
		this.setCurrentPlayer(this.getPlayerContainer().nextPlayer(currentPlayer));
		
		//TODO this needs to be called whenever a player busts or a player stays
		//TODO this needs to tell the server to tell the next player its his turn
		//After Bean
	}
	
	
	
	
	//method that goes through playerContainer and compares each players hands (possibly multiple) with the hand of the dealrs
	//This method will determine which players hands have lost (and thus that money is subtracted from their chipCount
	//this method will determine which players hands have won (and thus the money they bet on that hand is not subtracted from their chipcount and that same amount is added to their chipcount)
	//this method will determine which players hands pushed (and thus the chipcount does not change for this hand) 
	
	//this method should only be called if the dealer did not have a blackjack at the beginning of the round
	public void determineWinnersAndLosers()
	{
		//if dealer busts then everyone who has not busted wins
		
		//this may not be necessary
		//TODO this may be a repeat Todo, but, make sure that i am recalculating the dealer's hand whenever he gets a card to make sure i am not screwing up 
		dealer.getDealerHand().calculateHandValue();
		
		if (dealer.getDealerHand().isBusted() == true){
			dealerBustPlayerOutcome();
		}else{

			//if dealer has not busted then you compare
			dealerNoBustPlayerOutcome(); 
		}
	}

	public void dealerBustPlayerOutcome() 
	{
		if(dealer.getDealerHand().isBusted() == false){
			throw new UnsupportedOperationException(); 
		}
		for(int i = 0; i < this.getPlayerContainer().getPlayerContainer().size(); i++)
		{
			for(int j = 0; j < this.getPlayerContainer().getPlayerContainer().get(i).getHands().size(); j++)
			{
				Player currentPlayer = this.getPlayerContainer().getPlayerContainer().get(i);
				if (currentPlayer.getHands().get(j).isBusted() == false){
					//player wins his money 
					currentPlayer.setChipCount(currentPlayer.getChipCount() + currentPlayer.getHands().get(j).getCurrentBet());						
				}
				else{
					//player loses his money
					currentPlayer.setChipCount(currentPlayer.getChipCount() - currentPlayer.getHands().get(j).getCurrentBet());
				}
			}
		}
	}
	
	public void dealerNoBustPlayerOutcome()
	{
		int dealerValue = 0; 
		if (this.getDealer().getDealerHand().getHighestValue() > 21){
			dealerValue = this.getDealer().getDealerHand().getLowestValue();
		}
		else{
			dealerValue = this.getDealer().getDealerHand().getHighestValue();
		}
		for(int i = 0; i < this.getPlayerContainer().getPlayerContainer().size(); i++)
		{
			for(int j = 0; j < this.getPlayerContainer().getPlayerContainer().get(i).getHands().size(); j++)
			{
				Player currentPlayer = this.getPlayerContainer().getPlayerContainer().get(i);
				int currentHandValue = currentPlayer.getHands().get(j).getRealValue();
				if(currentHandValue > 21){
					currentPlayer.setChipCount(currentPlayer.getChipCount() - currentPlayer.getHand(j).getCurrentBet());
				}
				else if(currentHandValue > dealerValue){
					currentPlayer.setChipCount(currentPlayer.getChipCount() + currentPlayer.getHands().get(j).getCurrentBet());						
				}else if(currentHandValue == dealerValue){
					//this means the player has pushed with the dealer
					//nothing happens
				}else{
					currentPlayer.setChipCount(currentPlayer.getChipCount() - currentPlayer.getHands().get(j).getCurrentBet());
				}
			}
		}
		
	}
	
	public void roundIsOver()
	{
		//reset things for each player
		for (int i = 0; i < playerContainer.getPlayerContainer().size(); i++)
		{
			Player tempCurrentPlayer = playerContainer.getPlayerContainer().get(i);
			for(int j = 0; j < tempCurrentPlayer.getHands().size(); j++)
			{
				tempCurrentPlayer.getHands().get(j).setCurrentBet(0);
				
			}
			tempCurrentPlayer.getHands().clear(); 
			tempCurrentPlayer.setCurrentHand(null);
			
	
		}
		
		//reset things for the dealer
		dealer.setCurrentPlayer(null);
		dealer.setDealerHand(null);
		dealer.evaluateDeck();
		
		
		//reset things for the gameplay
		this.setCurrentPlayer(null);
		
		
	}
	//server will have to tell me how many players there are in the lobby
	//this basically acts like a constructor to the entire poker game
	public void brandNewGameStarting(int numPlayers)
	{
		// initialize the playerContainer, dealer, and state (and thus, the gameplay) 
		playerContainer = new PlayerContainer(); 
		dealer = new Dealer();
		this.setDealer(dealer);
		this.setPlayerContainer(playerContainer);
		currentState = StateOfRound.BETTING;
		currentPlayer = null;
		
		
		
		//set things up for each player
		for(int i = 0; i < numPlayers; i++)
		{
			Player player = new Player(); 
			this.addPlayer(player);			
		}		
		
	}
	
	public Player getPlayer(int index)
	{
		return this.getPlayerContainer().getPlayerContainer().get(index);
	}
	
	public void beginRound()
	{
		this.setCurrentPlayer(playerContainer.getPlayer(0));
		this.setCurrentState(StateOfRound.BETTING);
		//players make bets
		
		//players are dealt cards
		
		//dealer is dealt cards
		
		//check for dealer blackjack
		
		//if dealer doesn't have blackjack then continue to playerActions
	}
	

	
		
}
