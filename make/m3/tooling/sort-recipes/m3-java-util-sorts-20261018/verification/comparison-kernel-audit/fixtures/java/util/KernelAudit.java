package java.util;
public class KernelAudit {
 static final Random R = new Random(20261018L);
 static long checks;
 static final EnumSet<SortAlgorithm> STABLE=EnumSet.of(SortAlgorithm.TIM,SortAlgorithm.MERGE,SortAlgorithm.BOTTOM_UP_MERGE,SortAlgorithm.NATURAL_MERGE,SortAlgorithm.IN_PLACE_MERGE,SortAlgorithm.INSERTION,SortAlgorithm.BINARY_INSERTION,SortAlgorithm.BUBBLE,SortAlgorithm.COCKTAIL,SortAlgorithm.GNOME,SortAlgorithm.ODD_EVEN,SortAlgorithm.TREE,SortAlgorithm.TOURNAMENT,SortAlgorithm.PATIENCE,SortAlgorithm.STRAND);
 static void check(SortAlgorithm alg, int[] keys, int[] input, boolean descending) {
  int[] actual=input.clone();
  Integer[] expected=Arrays.stream(input).boxed().toArray(Integer[]::new);
  Comparator<Integer> cmp=(l,r)-> {
   int order=descending?Integer.compare(keys[r],keys[l]):Integer.compare(keys[l],keys[r]);
   return order<0?Integer.MIN_VALUE:order>0?Integer.MAX_VALUE:0;
  };
  Arrays.sort(expected,cmp);
  M3SortContext context=new M3SortContext((l,r)->cmp.compare(l,r),200000000L);
  try { M3ComparisonSorts.sort(actual,alg,context); }
  catch(Throwable t) {throw new AssertionError(alg+" n="+keys.length+" keys="+Arrays.toString(keys)+" input="+Arrays.toString(input),t);}
  for(int i=1;i<actual.length;i++) if(cmp.compare(actual[i-1],actual[i])>0) throw new AssertionError(alg+" unsorted n="+keys.length+" keys="+Arrays.toString(keys)+" input="+Arrays.toString(input)+" actual="+Arrays.toString(actual));
  int[] membership=actual.clone(), expectedMembership=input.clone(); Arrays.sort(membership);Arrays.sort(expectedMembership);
  if(!Arrays.equals(membership,expectedMembership))throw new AssertionError(alg+" not a permutation");
  if(STABLE.contains(alg)) for(int i=0;i<actual.length;i++) if(actual[i]!=expected[i])throw new AssertionError(alg+" unstable keys="+Arrays.toString(keys)+" input="+Arrays.toString(input)+" actual="+Arrays.toString(actual)+" expected="+Arrays.toString(expected));
  checks++;
 }
 static int[] identity(int n){int[] a=new int[n];for(int i=0;i<n;i++)a[i]=i;return a;}
 static void shuffle(int[] a){for(int i=a.length-1;i>0;i--){int j=R.nextInt(i+1),t=a[i];a[i]=a[j];a[j]=t;}}
 public static void main(String[] args) {
  for(SortAlgorithm alg:SortAlgorithm.values()) {
   long before=checks;
   for(int n=0;n<=7;n++) {
    int variants=1;for(int i=0;i<n;i++)variants*=3;
    for(int pattern=0;pattern<variants;pattern++) {
     int[] keys=new int[n];int v=pattern;for(int i=0;i<n;i++){keys[i]=v%3;v/=3;}
     check(alg,keys,identity(n),false);
    }
   }
   for(int rep=0;rep<160;rep++) {
    int n=R.nextInt(300);int[] keys=new int[n],input=identity(n);
    for(int i=0;i<n;i++)keys[i]=switch(rep%7){case 0->0;case 1->i;case 2->-i;case 3->R.nextInt(7);case 4->i<(n>>>1)?i:n-i;case 5->R.nextBoolean()?Integer.MIN_VALUE:Integer.MAX_VALUE;default->R.nextInt();};
    shuffle(input);
    if(rep%9==0 && n>0)for(int i=0;i<n;i++)input[i]=R.nextInt(n);
    check(alg,keys,input,(rep&1)!=0);
   }
   int[] keys=new int[513];for(int i=0;i<keys.length;i++)keys[i]=R.nextInt();
   check(alg,keys,identity(keys.length),false);
   M3SortContext cancellation=new M3SortContext((l,r)->Integer.compare(keys[l],keys[r]),7);
   try{M3ComparisonSorts.sort(identity(keys.length),alg,cancellation);throw new AssertionError(alg+" did not checkpoint");}catch(M3SortContext.Stop expected){}
   System.out.println(alg+" "+(checks-before)+" cases PASS");
  }
  for(SortAlgorithm alg:new SortAlgorithm[]{SortAlgorithm.BITONIC,SortAlgorithm.ODD_EVEN_MERGE}){
   for(int n=0;n<=16;n++)for(int mask=0;mask<(1<<n);mask++){
    int[] keys=new int[n];for(int i=0;i<n;i++)keys[i]=(mask>>>i)&1;
    check(alg,keys,identity(n),false);
   }
   for(int n:new int[]{31,32,33,63,64,65,127,128,129,255,256,257,511,512,513,1023,1024,1025,4095,4096,4097}){
    int[] keys=new int[n];for(int i=0;i<n;i++)keys[i]=R.nextInt();check(alg,keys,identity(n),false);
   }
   System.out.println(alg+" zero-one network and boundary audit PASS");
  }
  for(SortAlgorithm alg:new SortAlgorithm[]{SortAlgorithm.TIM,SortAlgorithm.QUICK,SortAlgorithm.INTRO,SortAlgorithm.DUAL_PIVOT_QUICK,SortAlgorithm.MERGE,SortAlgorithm.BOTTOM_UP_MERGE,SortAlgorithm.NATURAL_MERGE,SortAlgorithm.IN_PLACE_MERGE,SortAlgorithm.HEAP,SortAlgorithm.SHELL,SortAlgorithm.TREE,SortAlgorithm.TOURNAMENT,SortAlgorithm.PATIENCE}){
   for(int shape=0;shape<5;shape++){
    int n=40001;int[] keys=new int[n],input=identity(n);
    for(int i=0;i<n;i++)keys[i]=switch(shape){case 0->i;case 1->-i;case 2->7;case 3->R.nextInt(17);default->R.nextInt();};
    check(alg,keys,input,false);
   }
   System.out.println(alg+" 40001-element shape audit PASS");
  }
  System.out.println("AUDIT_ONLY total cases="+checks+" PASS; production API/monitor/recipe qualification not implied");
 }
}
