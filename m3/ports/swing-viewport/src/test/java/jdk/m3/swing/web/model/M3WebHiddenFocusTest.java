/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.swing.web.model;

import static jdk.m3.swing.web.model.M3WebStates.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Focus decoration uses the same primitive HIDDEN lane as emission/navigation, without new hooks. */
public final class M3WebHiddenFocusTest {
 private static void need(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 public static final class Probe extends M3WebModel {
  int emissions,isCalls,rectCalls,radiusCalls,themeCalls,action,activated=-1;
  boolean useIs,useRect;RuntimeException isFailure,rectFailure,emitFailure,radiusFailure,themeFailure;
  Probe(){super("focus-probe",0,0,RECT,V_DEFAULT);}
  @Override public int aspects(){return A_D;}
  public void load(String sample){clear();for(String text:sample.split(";"))add(text);}
  @Override protected void doLayout(){for(int i=0;i<count;i++)setRect(i,10+i*30,12,16,18);}
  @Override protected void emit(M3WebPaint p){emissions++;p.rect(1,2,3,4,0x123456);if(useIs)is(0,HIDDEN);if(useRect)itemRect(0,0);if(emitFailure!=null)throw emitFailure;
   switch(action){case 1:flags[0]|=HIDDEN;break;case 2:flags[0]&=~HIDDEN;break;case 3:focus=1;break;case 4:focus=Integer.MAX_VALUE;break;case 5:count=1;break;case 6:focus=1;flags[1]|=HIDDEN;break;default:break;}}
  @Override public boolean is(int item,int state){isCalls++;if(isFailure!=null)throw isFailure;return super.is(item,state);}
  @Override public int itemRect(int item,int part){rectCalls++;if(rectFailure!=null)throw rectFailure;return super.itemRect(item,part);}
  @Override protected int radius(){radiusCalls++;if(radiusFailure!=null)throw radiusFailure;return 7;}
  @Override public M3WebTheme theme(){themeCalls++;if(themeFailure!=null)throw themeFailure;return super.theme();}
  @Override protected void activate(int item){activated=item;super.activate(item);}
  int accent(){return theme.slot(M3WebTheme.ACCENT);}
  void rawFocus(int item){focus=item;}
  void noFlags(){flags=null;}
 }
 private static Probe sample(){Probe m=new Probe();m.load("A;B");m.layout(160,90);m.setFocus(0);return m;}
 private static M3WebPaint paint(M3WebModel m){M3WebPaint p=new M3WebPaint();m.paint(p);return p;}
 private static int[] records(M3WebPaint p){int[] a=new int[p.count()*M3WebPaint.STRIDE];for(int i=0;i<p.count();i++)for(int k=0;k<M3WebPaint.STRIDE;k++)a[i*M3WebPaint.STRIDE+k]=p.get(i,k);return a;}
 private static void noNewHooks(Probe m){need(m.isCalls==0&&m.rectCalls==0&&m.themeCalls==0,"NEW_VIRTUAL_PAINT_HOOK");}
 private static M3WebPaint cardWithoutFocus(M3CardModel m){m.setState(-1,FOCUS,false);M3WebPaint p=paint(m);m.setState(-1,FOCUS,true);return p;}
 @Test public void hiddenFocusWithNonzeroGeometryAddsNoRing(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);need(m.itemRect(0,2)>0&&m.itemRect(0,3)>0,"RETAINED_NONZERO_GEOMETRY");m.rectCalls=0;M3WebPaint p=paint(m);need(p.count()==1&&m.emissions==1&&m.radiusCalls==0,"HIDDEN_FOCUS_RING_RECORDED");noNewHooks(m);}
 @Test public void stockCardHiddenFocusAddsNoRing(){M3CardModel m=new M3CardModel();m.layout(360,240);m.load("A~body;B~visible body");m.setFocus(0);m.setState(0,HIDDEN,true);need(m.itemRect(0,3)==0&&m.itemRect(0,2)>0,"STOCK_ZERO_HEIGHT_RETAINED_WIDTH");M3WebPaint without=cardWithoutFocus(m),with=paint(m);need(Arrays.equals(records(without),records(with))&&with.count()>0,"STOCK_HIDDEN_PADDED_FOCUS_RING");}
 @Test public void stockCardHideShowRetainsFocusAndRestoresBytes(){M3CardModel m=new M3CardModel();m.layout(360,240);m.load("A~body;B~body");m.setFocus(0);m.setState(-1,FOCUS,true);int[] visible=records(paint(m));m.setState(0,HIDDEN,true);M3WebPaint without=cardWithoutFocus(m);need(m.focus()==0&&Arrays.equals(records(without),records(paint(m))),"HIDE_MOVED_FOCUS_OR_RECORDED_RING");m.setState(0,HIDDEN,false);need(m.focus()==0&&Arrays.equals(visible,records(paint(m))),"SHOW_DID_NOT_RESTORE_EXACT_VISIBLE_BYTES");}
 @Test public void visibleFocusBytesAndStateAreUnchanged(){Probe m=sample();m.setState(-1,FOCUS,true);int[] before=m.snapshot();int gen=m.generation();M3WebPaint p=paint(m);need(p.count()==2&&p.get(0,M3WebPaint.F_OP)==M3WebPaint.RECT&&p.get(1,M3WebPaint.F_OP)==M3WebPaint.ROUND_LINE&&p.get(1,M3WebPaint.F_X)==8&&p.get(1,M3WebPaint.F_Y)==10&&p.get(1,M3WebPaint.F_W)==20&&p.get(1,M3WebPaint.F_H)==22&&p.get(1,M3WebPaint.F_ARG)==9&&p.get(1,M3WebPaint.F_RGB)==m.accent(),"VISIBLE_FOCUS_BYTES_CHANGED");need(Arrays.equals(before,m.snapshot())&&m.generation()==gen&&m.focus()==0&&m.radiusCalls==1,"PAINT_MUTATED_MODEL");noNewHooks(m);}
 @Test public void widgetHiddenResetsBeforeAnyEmitterOrHook(){Probe m=sample();m.setState(-1,FOCUS|HIDDEN,true);m.isFailure=new IllegalStateException("is");m.rectFailure=new IllegalStateException("rect");m.emitFailure=new IllegalStateException("emit");m.noFlags();M3WebPaint p=new M3WebPaint();p.rect(1,1,1,1,1);m.paint(p);need(p.count()==0&&m.emissions==0&&m.radiusCalls==0,"WIDGET_HIDDEN_DID_WORK");noNewHooks(m);}
 @Test public void zeroViewportResetsBeforeAnyEmitterOrHook(){Probe m=sample();m.setState(-1,FOCUS,true);m.layout(0,0);m.emitFailure=new IllegalStateException("emit");m.noFlags();M3WebPaint p=new M3WebPaint();p.rect(1,1,1,1,1);m.paint(p);need(p.count()==0&&m.emissions==0&&m.radiusCalls==0,"EMPTY_VIEW_DID_WORK");noNewHooks(m);}
 @Test public void invalidFocusBoundsPrecedeFlagAccess(){for(int index:new int[]{2,Integer.MAX_VALUE}){Probe m=sample();m.rawFocus(index);m.setState(-1,FOCUS,true);m.noFlags();need(paint(m).count()==1&&m.emissions==1&&m.radiusCalls==0,"INVALID_FOCUS_NOT_BOUNDED");noNewHooks(m);}}
 @Test public void absentItemFocusDoesNotReadFlags(){Probe m=sample();m.rawFocus(-1);m.setState(-1,FOCUS,true);m.noFlags();need(paint(m).count()==1&&m.radiusCalls==0,"NO_FOCUS_DECORATION");noNewHooks(m);}
 @Test public void absentWidgetFocusDoesNotReadFlagsOrCallIs(){Probe m=sample();m.noFlags();m.isFailure=new IllegalStateException("is");m.rectFailure=new IllegalStateException("rect");need(paint(m).count()==1&&m.radiusCalls==0,"WIDGET_FOCUS_SHORT_CIRCUIT_CHANGED");noNewHooks(m);}
 @Test public void visibleFocusDoesNotIntroduceVirtualQueries(){Probe m=sample();m.setState(-1,FOCUS,true);m.isFailure=new IllegalStateException("is");m.rectFailure=new IllegalStateException("rect");m.themeFailure=new IllegalStateException("theme");need(paint(m).count()==2,"VISIBLE_PAINT_FAILED");noNewHooks(m);RuntimeException marker=new IllegalStateException("radius");m.radiusFailure=marker;M3WebPaint p=new M3WebPaint();try{m.paint(p);throw new AssertionError("EXPECTED_RADIUS_THROW");}catch(RuntimeException caught){need(caught==marker&&p.count()==1,"RADIUS_ERROR_ORDER");}noNewHooks(m);}
 @Test public void nullResetPrecedesEmitterAndVirtualQueries(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);m.isFailure=new IllegalStateException("is");m.rectFailure=new IllegalStateException("rect");m.emitFailure=new IllegalStateException("emit");try{m.paint(null);throw new AssertionError("EXPECTED_NULL_RESET");}catch(NullPointerException expected){need(m.emissions==0&&m.radiusCalls==0,"NULL_BEFORE_EMIT");}noNewHooks(m);}
 @Test public void emitterIsFailurePrecedesFocusFiltering(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);RuntimeException marker=new IllegalStateException("emit is");m.isFailure=marker;m.useIs=true;M3WebPaint p=new M3WebPaint();try{m.paint(p);throw new AssertionError("EXPECTED_EMIT_IS_THROW");}catch(RuntimeException caught){need(caught==marker&&p.count()==1&&m.isCalls==1&&m.rectCalls==0&&m.radiusCalls==0,"EMIT_IS_ERROR_ORDER");}}
 @Test public void emitterItemRectFailurePrecedesFocusFiltering(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);RuntimeException marker=new IllegalStateException("emit rect");m.rectFailure=marker;m.useRect=true;M3WebPaint p=new M3WebPaint();try{m.paint(p);throw new AssertionError("EXPECTED_EMIT_RECT_THROW");}catch(RuntimeException caught){need(caught==marker&&p.count()==1&&m.rectCalls==1&&m.isCalls==0&&m.radiusCalls==0,"EMIT_RECT_ERROR_ORDER");}}
 @Test public void emitterFailureKeepsIdentityAndRecordPrefix(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);RuntimeException marker=new IllegalStateException("emit");m.emitFailure=marker;M3WebPaint p=new M3WebPaint();try{m.paint(p);throw new AssertionError("EXPECTED_EMIT_THROW");}catch(RuntimeException caught){need(caught==marker&&p.count()==1&&m.radiusCalls==0,"EMIT_IDENTITY_OR_PREFIX");}noNewHooks(m);}
 @Test public void emitHidesFocusBeforeDecoration(){Probe m=sample();m.setState(-1,FOCUS,true);m.action=1;need(paint(m).count()==1&&m.focus()==0&&m.emissions==1&&m.radiusCalls==0,"POST_EMIT_HIDDEN_FOCUS_RING");noNewHooks(m);}
 @Test public void emitRevealsFocusBeforeDecoration(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);m.action=2;need(paint(m).count()==2&&m.focus()==0&&m.radiusCalls==1,"POST_EMIT_VISIBLE_FOCUS_SUPPRESSED");noNewHooks(m);}
 @Test public void emitMovesFocusBeforeDecoration(){for(int action:new int[]{3,6}){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);m.action=action;M3WebPaint p=paint(m);need(m.focus()==1,"POST_EMIT_FOCUS_IDENTITY");if(action==3)need(p.count()==2&&p.get(1,M3WebPaint.F_X)==38,"POST_EMIT_VISIBLE_TARGET");else need(p.count()==1&&m.radiusCalls==0,"POST_EMIT_HIDDEN_NEW_TARGET_RING");noNewHooks(m);}}
 @Test public void emitInvalidatesFocusBeforeFlagAccess(){Probe m=sample();m.setState(-1,FOCUS,true);m.action=4;need(paint(m).count()==1&&m.radiusCalls==0,"POST_EMIT_INVALID_FOCUS");noNewHooks(m);}
 @Test public void emitShrinksCountBeforeFlagAccess(){Probe m=sample();m.setFocus(1);m.setState(-1,FOCUS,true);m.action=5;need(paint(m).count()==1&&m.focus()==1&&m.count()==1&&m.radiusCalls==0,"POST_EMIT_COUNT_BOUND");noNewHooks(m);}
 @Test public void focusIdentityAndKeyboardSemanticsArePreserved(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);paint(m);need(m.focus()==0&&m.key(KEY_ENTER,0,0)&&m.activated==0,"EXISTING_HIDDEN_ENTER_IDENTITY");need(m.key(KEY_HOME,0,0)&&m.focus()==1&&m.key(KEY_END,0,0)&&m.focus()==1,"NAVIGATION_HIDDEN_SKIP");need(!m.key(KEY_LEFT,0,0)&&m.focus()==1,"EXISTING_PREVIOUS_BOUNDARY");}
 public static String visibleTranscript(){Probe m=sample();m.setState(-1,FOCUS,true);return Arrays.toString(records(paint(m)));}
 public static String keyboardTranscript(){Probe m=sample();m.setState(0,HIDDEN,true);m.setState(-1,FOCUS,true);paint(m);int first=m.focus();boolean enter=m.key(KEY_ENTER,0,0),home=m.key(KEY_HOME,0,0),end=m.key(KEY_END,0,0),previous=m.key(KEY_LEFT,0,0);return first+":"+enter+":"+m.activated+":"+home+":"+end+":"+previous+":"+m.focus();}
}
