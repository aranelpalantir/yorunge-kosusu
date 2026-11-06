import array, hashlib, io, math, wave, zipfile
from pathlib import Path
root=Path(__file__).resolve().parent.parent
names=['orbit.wav','ion.wav','drift.wav','turkish_march.wav','ode.wav','waltz.wav','pulsar.wav','nebula.wav','elise.wav']
hashes=set();durations=set()
with zipfile.ZipFile(root/'dist/Yorunge-Kosusu.apk') as apk:
    for name in names:
        data=apk.read('assets/'+name)
        assert data==(root/'assets'/name).read_bytes(),name+' stale APK asset'
        assert apk.getinfo('assets/'+name).compress_type==zipfile.ZIP_STORED,name+' cannot openFd compressed audio'
        with wave.open(io.BytesIO(data),'rb') as wav:
            assert wav.getframerate()==22050 and wav.getnchannels()==1 and wav.getsampwidth()==2
            samples=array.array('h',wav.readframes(wav.getnframes()))
            duration=len(samples)/wav.getframerate();durations.add(round(duration,2))
            assert 12<duration<30
            rms=math.sqrt(sum(x*x for x in samples)/len(samples))/32768
            assert .025<rms<.3 and max(abs(x) for x in samples)<32767
            assert abs(samples[0]-samples[-1])<1500,'Loop boundary discontinuity'
            hashes.add(hashlib.sha256(data).hexdigest())
            print(name+': %.2fs, RMS %.3f, no clipping, uncompressed APK asset'%(duration,rms))
    assert len(hashes)==9
    data=apk.read('assets/hit.wav')
    assert data==(root/'assets/hit.wav').read_bytes()
    with wave.open(io.BytesIO(data),'rb') as wav:
        samples=array.array('h',wav.readframes(wav.getnframes()))
        duration=len(samples)/wav.getframerate()
        rms=math.sqrt(sum(x*x for x in samples)/len(samples))/32768
        assert .75<duration<.95 and .08<rms<.24
        assert max(abs(x) for x in samples)<24000
        print('Fall effect: %.2fs, RMS %.3f, no clipping'%(duration,rms))
print('PASS: six distinct packaged music loops and clear gentle fall effect.')
