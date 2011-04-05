/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia;


import net.sourceforge.kolmafia.request.BasementRequest;

public class MonsterExpression
	extends Expression
{
	public MonsterExpression( String text, String name )
	{
		super( text, name );
	}

	protected boolean validBytecode( char inst )
	{
		return true;
	}

	protected float evalBytecode( char inst )
	{
		float v = 0.0f;
		switch ( inst )
		{
		case 'A':
			v = KoLCharacter.getAscensions();
			break;
		case '\u0080':
			v = KoLCharacter.getAdjustedMuscle();
			break;
		case '\u0081':
			v = KoLCharacter.getAdjustedMysticality();
			break;
		case '\u0082':
			v = KoLCharacter.getAdjustedMoxie();
			break;
		case '\u0083':
			v = KoLCharacter.getMonsterLevelAdjustment();
			break;
		case '\u0084':
			v = KoLCharacter.getMindControlLevel();
			break;
		case '\u0085':
			v = KoLCharacter.getMaximumHP();
			break;
		case '\u0086':
			v = BasementRequest.getBasementLevel();
			break;
		}
		return v;
	}

	protected String function()
	{
		if ( this.optional( "MUS" ) )
		{
			return "\u0080";
		}
		if ( this.optional( "MYS" ) )
		{
			return "\u0081";
		}
		if ( this.optional( "MOX" ) )
		{
			return "\u0082";
		}
		if ( this.optional( "ML" ) )
		{
			return "\u0083";
		}
		if ( this.optional( "MCD" ) )
		{
			return "\u0084";
		}
		if ( this.optional( "HP" ) )
		{
			return "\u0085";
		}
		if ( this.optional( "BL" ) )
		{
			return "\u0086";
		}

		return null;
	}
}
