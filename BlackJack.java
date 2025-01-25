import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class BlackJack {
    private class Card {
        String value;
        String type;

        Card(String value, String type) {
            this.value = value;
            this.type = type;
        }

        public String toString() {
            return value + "-" + type;
        }

        public int getValue() {
            if ("AJQK".contains(value)) {
                if (value.equals("A")) {
                    return 11;
                }
                return 10;
            }
            return Integer.parseInt(value);
        }

        public boolean isAce() {
            return value.equals("A");
        }

        public String getImagePath() {
            return "./cards/" + toString() + ".png";
        }
    }

    ArrayList<Card> deck;
    Random random = new Random();

    // Dealer
    Card hiddenCard;
    ArrayList<Card> dealerHand;
    int dealerSum;
    int dealerAceCount;

    // Player
    ArrayList<Card> playerHand;
    int playerSum;
    int playerAceCount;

    // Animation
    int animationStep = 0;

    // Window
    int boardWidth = 600;
    int boardHeight = boardWidth;

    int cardWidth = 110;
    int cardHeight = 154;

    JFrame frame = new JFrame("Black Jack");
    JPanel gamePanel = new JPanel() {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
        
            try {
                // Calculate the total width of all dealer's cards (hidden + visible)
                int totalDealerCards = dealerHand.size() + 1; // +1 for the hidden card
                int dealerCardsWidth = (totalDealerCards * (cardWidth + 5)) - 5;
        
                // Determine the starting X coordinate to center the dealer's cards
                int dealerStartX = (boardWidth - dealerCardsWidth) / 2;
        
                // Draw dealer's hidden card
                Image hiddenCardImg = new ImageIcon(getClass().getResource("./cards/BACK.png")).getImage();
                if (!stayButton.isEnabled()) {
                    hiddenCardImg = new ImageIcon(getClass().getResource(hiddenCard.getImagePath())).getImage();
                }
                g.drawImage(hiddenCardImg, dealerStartX, 20, cardWidth, cardHeight, null);
        
                // Draw dealer's visible cards
                for (int i = 0; i < dealerHand.size(); i++) {
                    if (animationStep > i + 1) {
                        Card card = dealerHand.get(i);
                        Image cardImg = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
                        g.drawImage(cardImg, dealerStartX + (cardWidth + 5) * (i + 1), 20, cardWidth, cardHeight, null);
                    }
                }
        
                // Draw player's hand
                int playerCardsWidth = (playerHand.size() * (cardWidth + 5)) - 5;
                int playerStartX = (boardWidth - playerCardsWidth) / 2; // Centering player cards
                for (int i = 0; i < playerHand.size(); i++) {
                    if (animationStep > i + 2) {
                        Card card = playerHand.get(i);
                        Image cardImg = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
                        g.drawImage(cardImg, playerStartX + (cardWidth + 5) * i, 320, cardWidth, cardHeight, null);
                    }
                }
        
                // Draw totals
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.setColor(Color.white);
        
                // Dealer total (hidden card consideration)
                String dealerTotalText = (!stayButton.isEnabled()) ? "Dealer Total: " + reduceDealerAce() : "Dealer Total: ?";
                g.drawString(dealerTotalText, 20, 200);
        
                // Player total
                if (animationStep > 4) {
                    g.drawString("Player Total: " + reducePlayerAce(), 20, 500);
                }
        
                // Outcome message
                if (!stayButton.isEnabled()) {
                    String message = "";
                    if (playerSum > 21) {
                        message = "You Lose!";
                        playSound("sound/playerbust.wav");
                    } else if (dealerSum > 21) {
                        message = "You Win!";
                        playSound("sound/dealerbust.wav");
                    } else if (playerSum == dealerSum) {
                        message = "Tie!";
                    } else if (playerSum > dealerSum) {
                        message = "You Win!";
                        playSound("sound/playerwins.wav");
                    } else {
                        message = "You Lose!";
                        playSound("sound/bankerwins.wav");
                    }
        
                    g.setFont(new Font("Arial", Font.PLAIN, 30));
                    g.drawString(message, 220, 250);
                    restartGame();
                }
        
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    };

    JPanel buttonPanel = new JPanel();
    JButton hitButton = new JButton("Hit");
    JButton stayButton = new JButton("Stay");

    Timer flipTimer;
    Clip currentClip;
    boolean welcomeSoundPlayed = false; // Flag to ensure welcome sound plays only once

    BlackJack() {
        startGame();
        playRandomStartSound();

        frame.setVisible(true);
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel.setLayout(new BorderLayout());
        gamePanel.setBackground(new Color(53, 101, 77));
        frame.add(gamePanel);

        hitButton.setFocusable(false);
        buttonPanel.add(hitButton);
        stayButton.setFocusable(false);
        buttonPanel.add(stayButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        hitButton.addActionListener(e -> {
            // Only allow hitting if the player's sum is less than 21
            if (playerSum < 21) {
                Card card = deck.remove(deck.size() - 1);
                playerSum += card.getValue();
                playerAceCount += card.isAce() ? 1 : 0;
                playerHand.add(card);
                // Only play "hitplayer" sound if the player's sum is less than 21 after hitting
                if (playerSum < 21) {
                    playSound("sound/hitplayer.wav");
                }
                if (reducePlayerAce() > 21) {
                    hitButton.setEnabled(false);
                    checkPlayerBust();
                }
                gamePanel.repaint();
                checkPlayerWins();
            }
        });

        stayButton.addActionListener(e -> {
            hitButton.setEnabled(false);
            stayButton.setEnabled(false);

            while (reduceDealerAce() < 17) {
                Card card = deck.remove(deck.size() - 1);
                dealerSum += card.getValue();
                dealerAceCount += card.isAce() ? 1 : 0;
                dealerHand.add(card);
            }
            gamePanel.repaint();
        });

        startFlippingAnimation();
    }

    public void startGame() {
        buildDeck();
        shuffleDeck();

        dealerHand = new ArrayList<>();
        dealerSum = 0;
        dealerAceCount = 0;

        hiddenCard = deck.remove(deck.size() - 1);
        dealerSum += hiddenCard.getValue();
        dealerAceCount += hiddenCard.isAce() ? 1 : 0;

        Card card = deck.remove(deck.size() - 1);
        dealerSum += card.getValue();
        dealerAceCount += card.isAce() ? 1 : 0;
        dealerHand.add(card);

        playerHand = new ArrayList<>();
        playerSum = 0;
        playerAceCount = 0;

        for (int i = 0; i < 2; i++) {
            card = deck.remove(deck.size() - 1);
            playerSum += card.getValue();
            playerAceCount += card.isAce() ? 1 : 0;
            playerHand.add(card);
        }
    }

    public void startFlippingAnimation() {
        flipTimer = new Timer(800, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                animationStep++;
                gamePanel.repaint();

                if (animationStep > 4) {
                    flipTimer.stop();
                }
            }
        });
        flipTimer.start();
    }

    public void buildDeck() {
        deck = new ArrayList<>();
        String[] values = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        String[] types = {"C", "D", "H", "S"};

        for (String type : types) {
            for (String value : values) {
                Card card = new Card(value, type);
                deck.add(card);
            }
        }
    }

    public void shuffleDeck() {
        for (int i = 0; i < deck.size(); i++) {
            int j = random.nextInt(deck.size());
            Card currCard = deck.get(i);
            Card randomCard = deck.get(j);
            deck.set(i, randomCard);
            deck.set(j, currCard);
        }
    }

    public int reducePlayerAce() {
        while (playerSum > 21 && playerAceCount > 0) {
            playerSum -= 10;
            playerAceCount--;
        }
        return playerSum;
    }

    public int reduceDealerAce() {
        while (dealerSum > 21 && dealerAceCount > 0) {
            dealerSum -= 10;
            dealerAceCount--;
        }
        return dealerSum;
    }

    public synchronized void playSound(String soundFile) {
        try {
            if (currentClip != null && currentClip.isOpen()) {
                currentClip.close(); // Ensure the previous clip is closed before playing a new one
            }
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource(soundFile));
            currentClip = AudioSystem.getClip();
            currentClip.open(audioInputStream);
            currentClip.start();
            currentClip.addLineListener(new LineListener() {
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        event.getLine().close();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playRandomStartSound() {
        if (!welcomeSoundPlayed) {
            String[] welcomeSounds = {"sound/welcomeplayer1.wav", "sound/welcomeplayer2.wav"};
            int randomIndex = random.nextInt(welcomeSounds.length);
            playSound(welcomeSounds[randomIndex]);
            welcomeSoundPlayed = true; // Ensure welcome sound only plays once
        }
    }

    public void checkPlayerWins() {
        if (playerSum == 21) {
            JOptionPane.showMessageDialog(frame, "Player wins!");
            playSound("sound/playerwins.wav");
            restartGame();
        }
    }

    public void checkPlayerBust() {
        if (playerSum > 21) {
            JOptionPane.showMessageDialog(frame, "You Lose!");
            playSound("sound/bankerwins.wav");
            revealDealerCard();
            restartGame();
        }
    }

    public void revealDealerCard() {
        // Reveal the dealer's hidden card
        stayButton.setEnabled(false);
        gamePanel.repaint();
    }

    public void restartGame() {
        startGame();
        hitButton.setEnabled(true);
        stayButton.setEnabled(true);
        animationStep = 0;
        startFlippingAnimation();
    }

    // Implementing Bubble Sort
    // Bubble Sort is a simple sorting algorithm that repeatedly steps through the list,
    // compares adjacent elements and swaps them if they are in the wrong order.
    public void bubbleSort(ArrayList<Card> cards) {
        int n = cards.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (cards.get(j).getValue() > cards.get(j + 1).getValue()) {
                    // Swap cards[j] and cards[j+1]
                    Card temp = cards.get(j);
                    cards.set(j, cards.get(j + 1));
                    cards.set(j + 1, temp);
                }
            }
        }
    }

    // Implementing Heap Sort
    // Heap Sort is a comparison-based sorting technique based on Binary Heap data structure.
    public void heapSort(ArrayList<Card> cards) {
        int n = cards.size();

        // Build heap (rearrange array)
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(cards, n, i);
        }

        // One by one extract an element from heap
        for (int i = n - 1; i > 0; i--) {
            // Move current root to end
            Card temp = cards.get(0);
            cards.set(0, cards.get(i));
            cards.set(i, temp);

            // Call max heapify on the reduced heap
            heapify(cards, i, 0);
        }
    }

    // To heapify a subtree rooted with node i which is an index in cards[]
    void heapify(ArrayList<Card> cards, int n, int i) {
        int largest = i; // Initialize largest as root
        int left = 2 * i + 1; // left = 2*i + 1
        int right = 2 * i + 2; // right = 2*i + 2

        // If left child is larger than root
        if (left < n && cards.get(left).getValue() > cards.get(largest).getValue()) {
            largest = left;
        }

        // If right child is larger than largest so far
        if (right < n && cards.get(right).getValue() > cards.get(largest).getValue()) {
            largest = right;
        }

        // If largest is not root
        if (largest != i) {
            Card swap = cards.get(i);
            cards.set(i, cards.get(largest));
            cards.set(largest, swap);

            // Recursively heapify the affected sub-tree
            heapify(cards, n, largest);
        }
    }

    public static void main(String[] args) {
        new BlackJack();
    }
}