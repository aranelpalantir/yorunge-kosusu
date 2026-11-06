"""Generate original synth music, public-domain arrangements and procedural effects (no third-party samples)."""
import math, wave, array, random
from pathlib import Path
RATE=22050
OUT=Path(__file__).resolve().parent.parent/'assets'
def save(name, samples):
    pcm=array.array('h',(int(max(-1,min(1,x))*28000) for x in samples))
    with wave.open(str(OUT/name),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE);f.writeframes(pcm.tobytes())
def freq(note):return 440*2**((note-69)/12)
beat=60/128
song=[0.0]*int(RATE*beat*32)
def tone(start,duration,note,volume):
    first=int(start*RATE);count=int(duration*RATE);hz=freq(note)
    for j in range(count):
        i=first+j
        if i>=len(song):break
        t=j/RATE;env=min(1,t/.008)*min(1,(duration-t)/.04)
        song[i]+=volume*env*(math.sin(2*math.pi*hz*t)+.2*math.sin(4*math.pi*hz*t))
melody=[72,76,79,76,74,76,81,79,69,72,76,72,71,72,76,79,65,69,72,69,67,69,74,72,67,71,74,79,77,74,71,74]
roots=[48,45,41,43]
for b in range(32):
    root=roots[(b//4)%4]
    tone(b*beat,beat*.72,root,.18)
    tone(b*beat,beat*.36,melody[(b*2)%32],.15)
    tone((b+.5)*beat,beat*.36,melody[(b*2+1)%32],.12)
    # Synthetic kick, clap and hi-hat.
    rng=random.Random(b)
    for j in range(int(.13*RATE)):
        i=int(b*beat*RATE)+j
        if i>=len(song):break
        t=j/RATE
        song[i]+=.24*math.exp(-t*28)*math.sin(2*math.pi*(70*t-90*t*t))
        if b%2:song[i]+=.10*math.exp(-t*40)*rng.uniform(-1,1)
    for offset in (0,.5):
        for j in range(int(.04*RATE)):
            i=int((b+offset)*beat*RATE)+j
            if i<len(song):song[i]+=.045*math.exp(-j/RATE*100)*rng.uniform(-1,1)
save('orbit.wav',song)
for name,duration,start,end in [('jump.wav',.17,460,1050),('turn.wav',.06,340,490),('hit.wav',.32,170,45),('finish.wav',.5,660,1320),('coin.wav',.11,980,1560),('frenzy.wav',.38,520,1480)]:
    result=[]
    for i in range(int(duration*RATE)):
        t=i/RATE;phase=2*math.pi*(start*t+(end-start)*t*t/(2*duration))
        env=min(1,t/.005)*(1-t/duration)**1.4
        result.append(.36*env*(math.sin(phase)+.25*math.sin(2*phase)))
    save(name,result)
print('Generated original 15-second 128 BPM loop and four effects.')
# Alternative original arrangements: different harmony, tempo, rhythm and voicing.
def alternate(filename,bpm,notes,bass,soft=False):
    global song,beat
    beat=60/bpm
    song=[0.0]*int(RATE*beat*32)
    for b in range(32):
        root=bass[(b//4)%len(bass)]
        if soft:
            if b%2==0:
                for interval in (12,19,24):tone(b*beat,beat*1.8,root+interval,.038)
            tone(b*beat,beat*.8,notes[b%len(notes)],.13)
            tone(b*beat,beat*.85,root,.12)
        else:
            tone(b*beat,beat*.28,root,.17)
            tone((b+.5)*beat,beat*.25,root+12,.10)
            for offset in (0,.25,.5,.75):tone((b+offset)*beat,beat*.18,notes[(b*4+int(offset*4))%len(notes)],.10)
        rng=random.Random(170+b)
        for j in range(int(.12*RATE)):
            i=int(b*beat*RATE)+j
            if i>=len(song):break
            t=j/RATE
            song[i]+=(.12 if soft else .23)*math.exp(-t*32)*math.sin(2*math.pi*(65*t-80*t*t))
            if b%2:song[i]+=(.035 if soft else .08)*math.exp(-t*50)*rng.uniform(-1,1)
        for offset in ((.5,) if soft else (0,.5)):
            for j in range(int(.025*RATE)):
                i=int((b+offset)*beat*RATE)+j
                if i<len(song):song[i]+=.025*math.exp(-j/RATE*120)*rng.uniform(-1,1)
    save(filename,song)
alternate('ion.wav',144,[76,79,83,86,74,78,81,85,72,76,79,83,71,74,78,81],[40,38,36,35])
alternate('drift.wav',108,[74,78,81,85,83,81,78,76,71,74,78,81,79,78,74,73],[38,35,43,45],True)
print('Generated Ion (144 BPM) and Night Flight (108 BPM).')
# Public-domain themes rendered with an original additive piano-like instrument.
def piano_note(start,duration,note,volume):
    if note is None:return
    first=int(start*RATE);count=int(duration*RATE);hz=freq(note)
    for j in range(count):
        i=first+j
        if i>=len(song):break
        t=j/RATE;attack=min(1,t/.004);release=min(1,(duration-t)/.04)
        value=math.sin(2*math.pi*hz*t)*math.exp(-t*2.3)
        value+=.35*math.sin(4*math.pi*hz*t)*math.exp(-t*5)
        value+=.14*math.sin(6*math.pi*hz*t)*math.exp(-t*8)
        song[i]+=volume*attack*release*value

def classical(filename,bpm,events,accompaniment,repeats=2):
    global song,beat
    beat=60/bpm;beats=sum(d for _,d in events)
    song=[0.0]*int(RATE*beat*beats*repeats)
    for repeat in range(repeats):
        cursor=repeat*beats
        for note,duration in events:
            notes=note if isinstance(note,tuple) else (note,)
            for n in notes:piano_note(cursor*beat,duration*beat*.94,n,.15 if len(notes)==1 else .095)
            cursor+=duration
        for at,notes,duration in accompaniment:
            for note in notes:piano_note((repeat*beats+at)*beat,duration*beat*.88,note,.075)
    save(filename,song)

# Mozart K.331, opening eight-bar phrase. Mutopia public-domain score,
# righta/lefta; grace notes omitted. This is a short arrangement, not the full work.
turk=[(n,.25) for n in [71,69,68,69]]
turk += [(72,.5),(None,.5)]+[(n,.25) for n in [74,72,71,72]]
turk += [(76,.5),(None,.5)]+[(n,.25) for n in [77,76,75,76]]
turk += [(n,.25) for n in [83,81,80,81,83,81,80,81]]
turk += [(84,1),(81,.5),(84,.5)]
for _ in range(2):turk += [(83,.5),((78,81),.5),((76,79),.5),((78,81),.5)]
turk += [(83,.5),((78,81),.5),((76,79),.5),((75,78),.5),(76,1)]
accomp=[]
for bar in range(7):
    base=1+bar*2;root=57 if bar<4 else 52;chord=(60,64) if bar<4 else (59,64)
    for step in range(4):accomp.append((base+step*.5,(root,) if step==0 else chord,.5))
accomp.append((15,(52,64),1))
classical('turkish_march.wav',112,turk,accomp)

# Beethoven, Ode to Joy: familiar opening theme with original simple accompaniment.
ode=[(n,1) for n in [64,64,65,67,67,65,64,62,60,60,62,64]]+[(64,1.5),(62,.5),(62,2)]
ode += [(n,1) for n in [64,64,65,67,67,65,64,62,60,60,62,64]]+[(62,1.5),(60,.5),(60,2)]
classical('ode.wav',120,ode,[(b,(48 if (b//4)%2==0 else 43,55),.8) for b in range(0,32,2)],1)

# Original 3/4 waltz, not attributed to a historical composer.
waltz=[(n,d) for n,d in [(76,1),(79,1),(84,1),(83,1.5),(79,.5),(76,1),(74,1),(77,1),(81,1),(79,2),(None,1),(72,1),(76,1),(79,1),(77,1.5),(74,.5),(71,1),(72,1),(74,1),(76,1),(72,2),(None,1)]]
classical('waltz.wav',126,waltz,[(bar*3+step,(48 if bar%2==0 else 43,) if step==0 else (60,64,67),.7) for bar in range(8) for step in range(3)])

# Three additional energetic tracks: Supernova (150 BPM), Crystal (118 BPM), and Beethoven Fur Elise excerpt (132 BPM).
alternate('pulsar.wav',150,[79,83,86,91,88,86,83,79,76,79,83,88,86,83,79,76],[43,40,36,38])
alternate('nebula.wav',118,[76,81,83,88,86,83,81,76,74,78,81,86,83,81,78,74],[45,41,38,40],True)
elise=[(n,.5) for n in [76,75,76,75,76,71,74,72]]+[(69,1),(None,.5),(60,.5),(64,.5),(69,.5),(71,1),(None,.5),(64,.5),(68,.5),(71,.5),(72,1),(None,.5),(64,.5)]
elise+=[(n,.5) for n in [76,75,76,75,76,71,74,72]]+[(69,1),(None,.5),(60,.5),(64,.5),(69,.5),(71,1),(None,.5),(64,.5),(72,.5),(71,.5),(69,2)]
classical('elise.wav',132,elise,[(b,(45,52,57) if (b//3)%2==0 else (40,52,56),1.2) for b in range(0,24,3)],2)

# Audible on small speakers without harsh high-mid whistle or crack:
# warm descending synth chord (fifth interval + soft sub-fundamental) and gentle air rush.
rng=random.Random(884);fall_sound=[];duration=.85
for i in range(int(duration*RATE)):
    t=i/RATE;envelope=min(1,t/.018)*(1-t/duration)**1.25
    f1=540-320*(t/duration)**.75
    f2=f1*1.498
    p1=2*math.pi*(540*t-320*duration/1.75*(t/duration)**1.75)
    p2=p1*1.498
    tone_part=.26*math.sin(p1)+.12*math.sin(p2)+.14*math.sin(p1*.5)
    thump=.15*math.exp(-t*28)*math.sin(2*math.pi*(145*t-50*t*t))
    rush=.035*math.exp(-t*3.5)*rng.uniform(-1,1)
    fall_sound.append(envelope*(tone_part+rush)+thump)
save('hit.wav',fall_sound)
print('Added Turkish March excerpt, Ode to Joy, original Star Waltz, and a softer clear fall effect.')
