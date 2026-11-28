import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

// Use the bundled runtime, without installing project dependencies.
const runtime = process.env.ELS_PRESENTATION_MODULES;
const skill = process.env.ELS_PRESENTATION_SKILL;
const python = process.env.ELS_PRESENTATION_PYTHON;
if (!runtime || !skill || !python) throw new Error('Set the three ELS_PRESENTATION_* build paths.');
process.env.RUNTIME_NODE_MODULES = runtime;
const { Presentation, PresentationFile, registerPdfFontFace } = await import(pathToFileURL(path.join(runtime, '@oai/artifact-tool/dist/artifact_tool.mjs')).href);
for (const [weight, file] of [[400,'arial.ttf'],[700,'arialbd.ttf']]) {
  registerPdfFontFace({family:'Arial',weight,style:'normal',source:new Uint8Array(await fs.readFile(path.join(process.env.WINDIR ?? 'C:/Windows','Fonts',file)))});
}
const root = process.cwd();
const staging = path.join(root, 'target/els49-corrections');
await fs.mkdir(staging, {recursive:true});
const data = JSON.parse(await fs.readFile('docs/presentation/els-49-deck.json', 'utf8'));
const deck = Presentation.create({slideSize:{width:1280,height:720}});
for (const [i, entry] of data.entries()) {
  const slide = deck.slides.add();
  slide.background.fill = '#0F172A';
  const text = (value, top, height, size, color, bold=false) => {
    const s = slide.shapes.add({geometry:'textbox',position:{left:80,top,width:1120,height},fill:'none',line:{fill:'none',width:0}});
    s.text=value; s.text.style={fontFamily:'Arial',fontSize:size,color,bold};
  };
  text(entry.title,70,130,i===0?'46pt':'34pt','#FFFFFF',true);
  for (const [line, value] of entry.body.split('\n').entries()) {
    text(value,235+line*65,60,'24pt','#CBD5E1');
  }
  text(`${i+1} / ${data.length}     ${entry.speaker} · ${entry.seconds}s`,630,45,'17pt','#94A3B8');
  slide.speakerNotes.text = `${entry.speaker}; target ${entry.seconds} seconds. ${entry.notes}`;
  const png = await deck.export({slide,format:'png',scale:1});
  await fs.writeFile(path.join(staging,`slide-${i+1}.png`),new Uint8Array(await png.arrayBuffer()));
}
const candidate = path.join(staging,'candidate.pptx');
await (await PresentationFile.exportPptx(deck)).save(candidate);
const {finalizePresentation} = await import(pathToFileURL(path.join(skill,'container_tools/artifact_tool_utils.mjs')).href);
await finalizePresentation({workspaceDir:root,candidatePath:candidate,finalPath:path.join(root,'docs/presentation/els-49-presentation.pptx'),pythonExecutable:python,
 integrityValidatorPath:path.join(skill,'container_tools/inspect_presentation_package_integrity.py'),layoutValidatorPath:path.join(skill,'container_tools/inspect_presentation_layout_geometry.py'),
 layoutArgs:['--expected-slide-size-emu','12192000,6858000','--validate-heading-fit'],explicitTotalSlideCount:12,fontPolicy:{basis:'design',families:['Arial']},verifyArtifactToolImport:true,
 receiptPath:path.join(staging,`validation-${process.pid}.json`)});
const pdf = await deck.export({format:'pdf'});
await fs.writeFile('docs/presentation/els-49-presentation.pdf',new Uint8Array(await pdf.arrayBuffer()));
console.log('Built and checked 12 slides, embedded notes and matching PDF.');
