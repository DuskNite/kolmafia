/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IHPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IHPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEHPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;

import java.util.ArrayList;

/**
 * A special class used as a holder class to hold all of the
 * items which are available for use as HP buffers.
 */

public abstract class HPRestoreItemList extends StaticEntity
{
	private static final HPRestoreItem REMEDY = new HPRestoreItem( "soft green echo eyedrop antidote", 0 );
	private static final HPRestoreItem TINY_HOUSE = new HPRestoreItem( "tiny house", 22 );

	public static final HPRestoreItem WALRUS = new HPRestoreItem( "tongue of the walrus", 35 );
	public static final HPRestoreItem OTTER = new HPRestoreItem( "tongue of the otter", 15 );

	private static final HPRestoreItem BANDAGES = new HPRestoreItem( "lasagna bandages", 24 );
	private static final HPRestoreItem COCOON = new HPRestoreItem( "cannelloni cocoon", Integer.MAX_VALUE );
	private static final HPRestoreItem NAP = new HPRestoreItem( "disco nap", 20 );
	private static final HPRestoreItem POWERNAP = new HPRestoreItem( "disco power nap", 40 );
	private static final HPRestoreItem PHONICS = new HPRestoreItem( "phonics down", 48 );
	private static final HPRestoreItem CAST = new HPRestoreItem( "cast", 17 );
	private static final HPRestoreItem ELIXIR = new HPRestoreItem( "Doc Galaktik's Homeopathic Elixir", 18 );
	private static final HPRestoreItem BALM = new HPRestoreItem( "Doc Galaktik's Restorative Balm", 13 );
	private static final HPRestoreItem UNGUENT = new HPRestoreItem( "Doc Galaktik's Pungent Unguent", 4 );

	public static final HPRestoreItem [] CONFIGURES = new HPRestoreItem [] { OTTER, REMEDY, TINY_HOUSE, COCOON,
		PHONICS, CAST, ELIXIR, BALM, UNGUENT, WALRUS, BANDAGES, POWERNAP, NAP };

	private static final HPRestoreItem SCROLL = new HPRestoreItem( "scroll of drastic healing", Integer.MAX_VALUE );
	private static final HPRestoreItem HERBS = new HPRestoreItem( "Medicinal Herb's medicinal herbs", Integer.MAX_VALUE );
	private static final HPRestoreItem OINTMENT = new HPRestoreItem( "Doc Galaktik's Ailment Ointment", 9 );

	public static final HPRestoreItem [] FALLBACKS = new HPRestoreItem[] { HERBS, SCROLL, OINTMENT };

	public static JCheckBox [] getCheckboxes()
	{
		String hpRestoreSetting = getProperty( "hpRestores" );
		JCheckBox [] restoreCheckbox = new JCheckBox[ CONFIGURES.length + FALLBACKS.length ];

		for ( int i = 0; i < CONFIGURES.length; ++i )
		{
			restoreCheckbox[i] = new JCheckBox( CONFIGURES[i].toString() );
			restoreCheckbox[i].setSelected( hpRestoreSetting.indexOf( CONFIGURES[i].toString() ) != -1 );
		}

		for ( int i = 0; i < FALLBACKS.length; ++i )
		{
			restoreCheckbox[CONFIGURES.length + i] = new JCheckBox( FALLBACKS[i].toString() );
			restoreCheckbox[CONFIGURES.length + i].setSelected( true );
			restoreCheckbox[CONFIGURES.length + i].setEnabled( false );
		}

		return restoreCheckbox;
	}

	public static class HPRestoreItem
	{
		private int skillID;
		private String itemName;
		private int hpPerUse;
		private AdventureResult itemUsed;

		public HPRestoreItem( String itemName, int hpPerUse )
		{
			this.itemName = itemName;
			this.hpPerUse = hpPerUse;
			this.skillID = ClassSkillsDatabase.getSkillID( itemName );
			this.itemUsed = new AdventureResult( itemName, 0 );
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public void recoverHP( int needed, boolean isFallback )
		{
			// Remedies are only used if the player is beaten up.
			// Otherwise, it is not used.

			if ( this == REMEDY )
			{
				if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new UneffectRequest( client, KoLAdventure.BEATEN_UP )).run();

				return;
			}

			// For all other instances, you will need to calculate
			// the number of times this technique must be used.

			int hpShort = needed - KoLCharacter.getCurrentHP();
			int belowMax = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
			int numberToUse = (int) Math.ceil( (double) hpShort / (double) hpPerUse );

			if ( ClassSkillsDatabase.contains( itemName ) )
			{
				if ( !KoLCharacter.hasSkill( itemName ) )
					numberToUse = 0;
			}
			else if ( TradeableItemDatabase.contains( itemName ) )
			{
				// In certain instances, you are able to buy more of
				// the given item from NPC stores, or from the mall.

				int numberAvailable = itemUsed.getCount( KoLCharacter.getInventory() );

				if ( !isFallback )
					numberAvailable = Math.min( numberToUse, numberAvailable );
				else if ( this == HERBS )
					numberAvailable = belowMax < 20 || !NPCStoreDatabase.contains( HERBS.toString() ) ? 0 : 1;
				else if ( this == SCROLL && KoLCharacter.canInteract() )
					numberAvailable = 1;
				else if ( this == OINTMENT )
					numberAvailable = numberToUse;

				numberToUse = Math.min( numberToUse, numberAvailable );
			}

			if ( numberToUse == 0 )
				return;

			if ( this == TINY_HOUSE )
			{
				if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new ConsumeItemRequest( client, new AdventureResult( "tiny house", 1 ) )).run();

				return;
			}

			if ( this == OTTER )
			{
				if ( KoLCharacter.getEffects().contains( KoLAdventure.BEATEN_UP ) )
					(new UseSkillRequest( client, toString(), "", 1 )).run();

				return;
			}

			if ( ClassSkillsDatabase.contains( this.toString() ) )
			{
				if ( this != COCOON || belowMax >= 20 )
					(new UseSkillRequest( client, this.toString(), "", numberToUse )).run();
			}
			else
			{
				(new ConsumeItemRequest( client, itemUsed.getInstance( numberToUse ) )).run();
			}
		}

		public String toString()
		{	return itemName;
		}
	}
}
