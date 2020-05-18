/**
 * Copyright (c) 2005-2020, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;

public abstract class TowerDoorManager
{
	// Items for the tower doorway
	private static final AdventureResult UNIVERSAL_KEY = ItemPool.get( ItemPool.UNIVERSAL_KEY, 1 );

	private static final Map<String, Lock> actionToLock = new HashMap<>();

	public static class Lock
	{
		final String name;		// The name of the lock
		final AdventureResult key;	// The key that fits the lock
		final String action;		// The action name for the lock
		final KoLAdventure location;	// Where to find the kay
		final String encounter;		// The Encounter that grants the key
		final boolean special;		// True if normal retrieve_item will not work
						// to get the key in Kingdom of Exploathing

		// Doorknobs and locks with "retrievable" keys
		public Lock( String name, int itemId, String action )
		{
			this.name = name;
			this.key = ( itemId == -1 ) ? null : ItemPool.get( itemId, 1 );
			this.action = action;
			this.location = null;
			this.encounter = null;
			this.special = false;
			TowerDoorManager.actionToLock.put( this.action, this );
		}

		// Locks with keys that may not be "retrievable"
		public Lock( String name, int itemId, String action, boolean special )
		{
			this.name = name;
			this.key = ( itemId == -1 ) ? null : ItemPool.get( itemId, 1 );
			this.action = action;
			this.location = null;
			this.encounter = null;
			this.special = special;
			TowerDoorManager.actionToLock.put( this.action, this );
		}

		// Low-Key Tower Door locks
		public Lock( String name, int itemId, String action, String location, String encounter )
		{
			this.name = name;
			this.key = ( itemId == -1 ) ? null : ItemPool.get( itemId, 1 );
			String prefix = "nstower_doowlow";
			this.action = prefix + action;
			this.location = AdventureDatabase.getAdventure( location );
			this.encounter = encounter;
			this.special = false;
			TowerDoorManager.actionToLock.put( this.action, this );
		}

		public String getName()
		{
			return this.name;
		}

		public AdventureResult getKey()
		{
			return this.key;
		}

		public String getAction()
		{
			return this.action;
		}

		public String getLocation()
		{
			return ( this.location == null ) ? "" : this.location.getAdventureName();
		}

		public String getEncounter()
		{
			return this.encounter;
		}

		public boolean isSpecial()
		{
			return this.special;
		}

		public boolean isDoorknob()
		{
			return this.key == null;
		}

		public boolean haveKey()
		{
			return this.key == null || this.key.getCount( KoLConstants.inventory ) > 0 || KoLCharacter.hasEquipped( this.key );
		}

		public boolean usedKey()
		{
			return Preferences.getString( "nsTowerDoorKeysUsed" ).contains( this.key.getName() );
		}

		public String keyEnchantments()
		{
			Modifiers mods = Modifiers.getItemModifiers( key.getItemId() );
			return mods == null ? "" :  mods.getString( "Modifiers" );
		}
	}

	// place.php?whichplace=nstower_door
	private static final Lock[] LOCK_DATA =
	{
		// Standard Locks:
		new Lock( "Boris's Lock", ItemPool.BORIS_KEY, "ns_lock1", true ),
		new Lock( "Jarlsberg's Lock", ItemPool.JARLSBERG_KEY, "ns_lock2", true ),
		new Lock( "Sneaky Pete's's Lock", ItemPool.SNEAKY_PETE_KEY, "ns_lock3", true ),
		new Lock( "Star Lock", ItemPool.STAR_KEY, "ns_lock4", false ),
		new Lock( "Digital Lock", ItemPool.DIGITAL_KEY, "ns_lock5", true ),
		new Lock( "Skeleton Lock", ItemPool.SKELETON_KEY, "ns_lock6", false ),
		// Doorknob
		new Lock( "Doorknob", -1, "ns_doorknob" ),
	};

	// place.php?whichplace=nstower_doorlowkey
	private static final Lock[] LOW_KEY_LOCK_DATA =
	{
		// Standard Locks:
		new Lock( "Boris's Lock", ItemPool.BORIS_KEY, "ns_lock1_lk" ),
		new Lock( "Jarlsberg's Lock", ItemPool.JARLSBERG_KEY, "ns_lock2_lk" ),
		new Lock( "Sneaky Pete's Lock", ItemPool.SNEAKY_PETE_KEY, "ns_lock3_lk" ),
		new Lock( "Star Lock", ItemPool.STAR_KEY, "ns_lock4_lk" ),
		new Lock( "Digital Lock", ItemPool.DIGITAL_KEY, "ns_lock5_lk" ),
		new Lock( "Skeleton Lock", ItemPool.SKELETON_KEY, "ns_lock6_lk" ),
		// Doorknob
		new Lock( "Doorknob", -1, "ns_doorknob_lk" ),
		// Low-Key Locks:
		new Lock( "Polka Dotted Lock", ItemPool.CLOWN_CAR_KEY, "key1", "The \"Fun\" House", "Carpool Lane" ),
		new Lock( "Bat-Winged Lock", ItemPool.BATTING_CAGE_KEY, "key2", "Bat Hole Entrance", "Batting a Thousand" ),
		new Lock( "Taco Locko", ItemPool.AQUI, "key3", "South of the Border", "Lost in Translation" ),
		new Lock( "Lockenmeyer Flask", ItemPool.KNOB_LABINET_KEY, "key4", "Cobb's Knob Laboratory", "F=ma" ),
		new Lock( "Antlered Lock", ItemPool.WEREMOOSE_KEY, "key5", "Cobb's Knob Menagerie, Level 2", "It's a Weremoose Key" ),
		new Lock( "Lock with one Eye", ItemPool.PEG_KEY, "key6", "The Obligatory Pirate's Cove", "Larrrst & Found" ),
		new Lock( "Trolling Lock", ItemPool.KEKEKEY, "key7", "The Valley of Rof L'm Fao", "Made of Stars" ),
		new Lock( "Rabbit-Eared Lock", ItemPool.RABBITS_FOOT_KEY, "key8", "The Dire Warren", "Lucky For You" ),
		new Lock( "Mine Cart Shaped Lock", ItemPool.KNOB_SHAFT_SKATE_KEY, "key9", "The Knob Shaft", "Getting the Shaft Key" ),
		new Lock( "Frigid Lock", ItemPool.ICE_KEY, "key10", "The Icy Peak", "OF Ice & Yetis" ),
		new Lock( "Anchovy Can", ItemPool.ANCHOVY_CAN_KEY, "key11", "The Haunted Pantry", "Hola, Amigos" ),
		new Lock( "Cactus-Shaped-Hole Lock", ItemPool.CACTUS_KEY, "key12", "The Arid, Extra-Dry Desert", "Midnight Sun" ),
		new Lock( "Boat Prow Lock", ItemPool.F_C_LE_SH_C_LE_K_Y, "key13", "The F'c'le", "B'c'le Up" ),
		new Lock( "Barnacley Lock", ItemPool.TREASURE_CHEST_KEY, "key14", "Belowdecks", "Yo-ho-ho in the Ho-ho-hold" ),
		new Lock( "Infernal Lock", ItemPool.DEMONIC_KEY, "key15", "Pandamonium Slums", "You Found a Thing, in Hell" ),
		new Lock( "Sausage With a Hole", ItemPool.KEY_SAUSAGE, "key16", "Cobb's Knob Kitchens", "Pork Key's Revenge" ),
		new Lock( "Golden Lock", ItemPool.KNOB_TREASURY_KEY, "key17", "Cobb's Knob Treasury", "Decisions, Shmecisions" ),
		new Lock( "Junky Lock", ItemPool.SCRAP_METAL_KEY, "key18", "The Old Landfill", "One Man's Trash is Presumably the Key to Another Man's Treasure" ),
		new Lock( "Spooky Lock", ItemPool.BLACK_ROSE_KEY, "key19", "The Haunted Conservatory", "Lore of the Roses" ),
		new Lock( "Crib-Shaped Lock", ItemPool.MUSIC_BOX_KEY, "key20", "The Haunted Nursery", "Jerk-in-the-Box" ),
		new Lock( "Boney Lock", ItemPool.ACTUAL_SKELETON_KEY, "key21", "The Skeleton Store", "Just Taking the Opportunity to be a Pedant" ),
		new Lock( "Loaf of Bread with Keyhole", ItemPool.DEEP_FRIED_KEY, "key22", "Madness Bakery", "Into the Fryer" ),
		new Lock( "Overgrown Lock", ItemPool.DISCARDED_BIKE_LOCK_KEY, "key23", "The Overgrown Lot", "Not a Lot Left" ),
	};

	public static Lock[] getLocks()
	{
		return KoLCharacter.isLowkey() ? LOW_KEY_LOCK_DATA : LOCK_DATA;
	}

	private static String getDoorPlace()
	{
		return KoLCharacter.isLowkey() ? "nstower_doorlowkey" : "nstower_door";
	}

	private static AdventureResult actionToKey( final String action )
	{
		Lock lock = TowerDoorManager.actionToLock.get( action );
		return lock == null ? null : lock.key;
	}

	// Frank looks at the lock. "Okay, Boss, for this you're gonna need
	// Boris's key. You know the guy -- Boris? Musclebound hero of the
	// Times of Old? There's a shrine to him over in the Dungeoneer's
	// Association. That's where I'd go, if I were you. Or even if I were
	// me and I were still alive."
	//
	// Frank looks at the complex lock. "Okay what we got here is
	// Jarlsberg's lock, and what you're gonna need is -- you guessed it --
	// Jarlsberg's key. Man, that guy was annoying. Always thinkin' he was
	// so smart. Anyway if I were you, I'd go check out the Dungeoneer's
	// Assocation in the mountains. They keep a shrine to the obnoxious
	// nerd there."
	//
	// Frank looks at the lock. "Oh man, Boss. For this you're gonna need
	// Sneaky Pete's key. That guy thought he was so cool. And, y'know, I
	// guess he was pretty cool. Anyway they got a shrine to him over at
	// the Dungeoneer's Association. I'd start there."
	//
	// Frank looks at the lock. "Okay, Chief. For this one, you're gonna
	// need a key made of stars. I know, I know, it sounded like hippy crap
	// to me, too. But you're gonna need to make your way to the Hole in
	// the Sky. Don't worry. It sounds scary, but it's actually hilarious."
	//
	// Frank looks at the lock. "Alright, Boss, this one's gonna be
	// interesting. You need a digital key. And the only way to get one is
	// to talk to that crackpot... er... crackpot in Forest Village. You
	// know the one. I'd point at myself and spin my finger in a circle if
	// I had a finger."
	//
	// Frank looks at the lock. "Heya Boss, for this one you're gonna need
	// a skeleton key. Get it? Well, no, if you had it you'd have used
	// it. You can make one out of bits of skeletons from the Cemetary."
	//
	// (Polka Dotted Lock)
	// (Bat-Winged Lock)
	// This lock looks like a taco. A lock-o taco.
	// (Lockenmeyer Flask)
	// (Antlered Lock)
	// (Lock with one Eye)
	// (Trolling Lock)
	// This lock is weirdly rabbit-shaped.
	// (Mine Cart Shaped Lock)
	// (Frigid Lock)
	// This lock is shaped like a can of anchovies. Spooky sardines.
	// This lock is shaped like a pyramid, and has a cactus-shaped keyhole.
	// (Boat Prow Lock)
	// (Barnacley Lock)
	// (Infernal Lock)
	// (Sausage with a Hole)
	// (Golden Lock)
	// (Junky Lock)
	// This lock is shaped like a haunted flower vase.
	// (Crib-Shaped Lock)
	// This lock is shaped like a ribcage with a pricetag on it..
	// This lock is shaped like a loaf of mentally-unstable bread.
	// This lock is made of grass and weeds.

	public static void parseTowerDoorResponse( final String action, final String responseText )
	{
		if ( action == null || action.equals( "" ) )
		{
			TowerDoorManager.parseTowerDoor( responseText );
			return;
		}

		if ( action.equals( "ns_doorknob" ) || action.equals( "ns_doorknob_lk" ) )
		{
			// You reach for the doorknob, but then you remember some unfinished business that you need to finish up for the council.
			// You turn the knob and the door vanishes. I guess it was made out of the same material as those weird lock plates.
			if ( responseText.contains( "You turn the knob and the door vanishes" ) )
			{
				QuestDatabase.setQuestProgress( Quest.FINAL, "step6" );
			}
			return;
		}

		AdventureResult key = TowerDoorManager.actionToKey( action );

		if ( key == null )
		{
			return;
		}

		AdventureResult item =
			responseText.contains( "universal key" ) ?
			TowerDoorManager.UNIVERSAL_KEY :
			key;

		// Boris's Lock: You place Boris's key in the lock and turn
		// it. You hear a jolly bellowing in the distance as the lock
		// vanishes, along with the metal plate it was attached
		// to. Huh.

		// Jarlsberg's Lock: You put Jarlsberg's key in the lock and
		// turn it. You hear a nasal, sort of annoying laugh in the
		// distance as the lock vanishes in a puff of
		// rotten-egg-smelling smoke.

		// Sneaky Pete's Lock: You put the key in the lock and hear the
		// roar of a motorcycle behind you. By the time you turn around
		// to check out the cool motorcycle guy he's gone, but when you
		// turn back to the lock it is <i>also</i> gone.

		// Star Lock: You put the key in and turn it. There is a flash
		// of brilliant starlight accompanied by a competent but not
		// exceptional drum solo, and when both have faded, the lock is
		// gone.

		// Skeleton Lock: You put the skeleton key in the lock and turn
		// it. The key, the lock, and the metal plate the lock is
		// attached to all crumble to dust. And rust, in the case of
		// the metal.

		// Digital Lock: You put the digital key in the lock and turn
		// it. A familiar sequence of eight tones plays as the lock
		// disappears.

		// *** Need the responses for the 23 new Low-Key locks:
		// Polka Dotted Lock
		// Bat-Winged Lock
		// Taco Locko
		// Lockenmeyer Flask
		// Antlered Lock
		// Lock with one Eye
		// Trolling Lock
		// Rabbit-Eared Lock
		// Mine Cart Shaped Lock
		// Frigid Lock
		// Anchovy Can
		// Cactus-Shaped-Hole Lock
		// Boat Prow Lock
		// Barnacley Lock
		// Infernal Lock
		// Sausage With a Hole
		// Golden Lock
		// Junky Lock
		// Spooky Lock
		// Crib-Shaped Lock
		// Boney Lock
		// Loaf of Bread with Keyhole
		// Overgrown Lock

		if ( responseText.contains( "the lock vanishes" ) ||
		     responseText.contains( "turn back to the lock" ) ||
		     responseText.contains( "the lock is gone" ) ||
		     responseText.contains( "crumble to dust" ) ||
		     responseText.contains( "the lock disappears" ) )
		{
			ResultProcessor.processResult( item.getNegation() );
			String keys = Preferences.getString( "nsTowerDoorKeysUsed" );
			Preferences.setString( "nsTowerDoorKeysUsed", keys + ( keys.equals( "" ) ? "" : "," ) + key.getDataName() );
		}
	}

	public static void parseTowerDoor( String responseText )
	{
		// Based on which locks are absent, deduce which keys have been used.

		StringBuilder buffer = new StringBuilder();

		for ( Lock lock : TowerDoorManager.getLocks() )
		{
			if ( lock.isDoorknob() )
			{
				continue;
			}

			if ( !responseText.contains( lock.action ) )
			{
				if ( buffer.length() > 0 )
				{
					buffer.append( "," );
				}
				buffer.append( lock.key.getDataName() );
			}
		}

		Preferences.setString( "nsTowerDoorKeysUsed", buffer.toString() );
	}

	public static boolean registerTowerDoorRequest( final String urlString )
	{
		String place = GenericRequest.getPlace( urlString );
		if ( place.equals( "nstower_door" ) && KoLCharacter.isLowkey() )
		{
			// This will redirect to "nstower_doorlowkey"
			return true;
		}

		String message = null;

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			String prefix = "[" + KoLAdventure.getAdventureCount() + "] ";
			RequestLogger.printLine();
			RequestLogger.updateSessionLog();
			message = prefix + "Tower Door";
		}
		else
		{
			Lock lock = TowerDoorManager.actionToLock.get( action );
			if ( lock == null )
			{
				return true;
			}
			message = "Tower Door: " + lock.name;
		}

		if ( message == null )
		{
			return true;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}

	public static final void towerDoorScript()
	{
		// Is the Tower Door open? Go look at the tower.
		RequestThread.postRequest( new PlaceRequest( "nstower" ) );

		String status = Quest.FINAL.getStatus();
		if ( !status.equals( "step5" ) )
		{
			String message =
				status.equals( QuestDatabase.UNSTARTED ) ?
				"You haven't been given the quest to fight the Sorceress!" :
				QuestDatabase.isQuestLaterThan( status, "step5" ) ?
				"You have already opened the Tower Door." :
				"You haven't reached the Tower Door yet.";

			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return;
		}

		String place = TowerDoorManager.getDoorPlace();
		Lock doorknob = null;

		// Look at the door to decide what remains to be done
		RequestThread.postRequest( new PlaceRequest( place ) );

		String keys = Preferences.getString( "nsTowerDoorKeysUsed" );

		ArrayList<Lock> needed = new ArrayList<Lock>();
		for ( Lock lock : TowerDoorManager.getLocks() )
		{
			if ( lock.isDoorknob() )
			{
				doorknob = lock;
				continue;
			}
			if ( !keys.contains( lock.key.getName() ) )
			{
				needed.add( lock );
			}
		}

		// If we have any locks left to open, acquire the correct key and unlock them
		if ( needed.size() > 0 )
		{
			// First acquire all needed keys
			CoinmasterData coinmaster = CoinmasterRegistry.findCoinmaster( "Cosmic Ray's Bazaar" );
			boolean exploathing = KoLCharacter.isKingdomOfExploathing();
			for ( Lock lock : needed )
			{
				AdventureResult key = lock.key;
				boolean have = InventoryManager.hasItem( key );
				if ( have )
				{
					// If we have the key, move it to inventory.
					// Otherwise, acquire it.
					have = InventoryManager.retrieveItem( key );
				}
				else if ( lock.location != null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Adventure in " + lock.location + " until you find a " + key );
					return;
				}
				else if ( lock.special && exploathing )
				{
					// We have to get this from Cosmic Ray's Bazaar
					AdventureResult[] itemList = new AdventureResult[1];
					itemList[0] = key;
					CoinMasterRequest request = coinmaster.getRequest( true, itemList );
					RequestThread.postRequest( request );
					have = InventoryManager.hasItem( key );
				}
				if ( !have )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to acquire " + key );
					return;
				}
			}

			// Then unlock each lock
			for ( Lock lock : needed )
			{
				RequestThread.postRequest( new PlaceRequest( place, lock.action ) );
				keys = Preferences.getString( "nsTowerDoorKeysUsed" );
				if ( !keys.contains( lock.key.getName() ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to open lock using " + lock.key );
					return;
				}
			}
		}

		// Now turn the doorknob
		RequestThread.postRequest( new PlaceRequest( place, doorknob.action, true ) );

		status = Quest.FINAL.getStatus();
		if ( status.equals( "step6" ) )
		{
			KoLmafia.updateDisplay( "Tower Door open!" );
		}
	}
}
