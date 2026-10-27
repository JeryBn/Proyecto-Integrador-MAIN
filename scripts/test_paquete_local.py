import json,subprocess,tempfile,unittest,zipfile
from pathlib import Path
from crear_paquete_local import build

class PackageTests(unittest.TestCase):
 def repo(self,folder):
  root=Path(folder);subprocess.run(['git','init','-q',str(root)],check=True)
  (root/'README.md').write_text('Fuente de prueba',encoding='utf-8');(root/'.gitignore').write_text('.env.local\nvar/\ntarget/\n')
  for args in [('add','.'),('-c','user.name=Synthetic','-c','user.email=synthetic@example.invalid','commit','-qm','fixture')]:
   subprocess.run(['git','-C',str(root),*args],check=True)
  (root/'target').mkdir();(root/'target/main-sprint1-0.1.0.jar').write_bytes(b'synthetic-jar')
  return root
 def test_ignored_files_never_enter_archive_and_manifest_matches(self):
  with tempfile.TemporaryDirectory() as folder:
   root=self.repo(folder);(root/'.env.local').write_text('PRIVATE_SENTINEL=not-a-real-secret')
   (root/'var').mkdir();(root/'var/config.json').write_text('PRIVATE_SENTINEL')
   result=build(root,root/'output')
   with zipfile.ZipFile(result['package']) as archive:
    self.assertNotIn('.env.local',archive.namelist());self.assertNotIn('var/config.json',archive.namelist())
    self.assertFalse(any(b'PRIVATE_SENTINEL' in archive.read(name) for name in archive.namelist()))
    manifest=json.loads(archive.read('MAIN_PACKAGE_MANIFEST.json'));self.assertEqual(manifest['commit'],result['commit'])
    self.assertFalse(manifest['androidRelease'])
 def test_dirty_source_is_not_mislabelled_as_a_commit(self):
  with tempfile.TemporaryDirectory() as folder:
   root=self.repo(folder);(root/'README.md').write_text('Cambio sin commit')
   with self.assertRaisesRegex(ValueError,'commit'):build(root,root/'output')
 def test_tracked_signing_material_is_excluded(self):
  with tempfile.TemporaryDirectory() as folder:
   root=self.repo(folder);(root/'test.keystore').write_text('synthetic signing material')
   subprocess.run(['git','-C',str(root),'add','test.keystore'],check=True)
   subprocess.run(['git','-C',str(root),'-c','user.name=Synthetic','-c','user.email=synthetic@example.invalid','commit','-qm','signing fixture'],check=True)
   result=build(root,root/'output')
   with zipfile.ZipFile(result['package']) as archive:self.assertNotIn('test.keystore',archive.namelist())
 def test_literal_key_parser_header_is_not_a_secret(self):
  with tempfile.TemporaryDirectory() as folder:
   root=self.repo(folder);(root/'README.md').write_text('Parser uses "-----BEGIN PRIVATE KEY-----" as a literal.')
   subprocess.run(['git','-C',str(root),'add','.'],check=True)
   subprocess.run(['git','-C',str(root),'-c','user.name=Synthetic','-c','user.email=synthetic@example.invalid','commit','-qm','literal fixture'],check=True)
   self.assertGreater(build(root,root/'output')['files'],0)
 def test_potential_credential_aborts_before_archive_is_written(self):
  with tempfile.TemporaryDirectory() as folder:
   root=self.repo(folder);(root/'accidental.txt').write_text('-----BEGIN PRIVATE KEY-----\n'+'A'*64+'\n-----END PRIVATE KEY-----')
   subprocess.run(['git','-C',str(root),'add','.'],check=True)
   subprocess.run(['git','-C',str(root),'-c','user.name=Synthetic','-c','user.email=synthetic@example.invalid','commit','-qm','fake key fixture'],check=True)
   with self.assertRaisesRegex(ValueError,'posible credencial'):build(root,root/'output')
   self.assertFalse((root/'output/MAIN_LOCAL_PAMELA.zip').exists())

if __name__=='__main__':unittest.main()
